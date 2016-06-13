package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.ClusterInfo
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries}
import team.supernova.graphite.MetricResult
import team.supernova.opscenter.OpsCenterClusterInfo

import scala.collection.JavaConverters._
import scala.collection.mutable

object ClusterInfoActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class WorkOnClusterFinished(cluster: ClusterEnv, clusterResults: Option[ClusterInfo])
  def props(requester: ActorRef): Props = Props(new ClusterInfoActor(requester))
}

case class ClusterGroupKey(clusterName: String, group: String) {
  override def toString = s"$group's $clusterName"
}

class ClusterInfoActor(requester: ActorRef) extends Actor with ActorLogging {
  import context._

  val metaActor = actorOf(ClusterMetadataActor.props(self))
  val slowQueryActor = actorOf(ClusterSlowQueryActor.props(self))
  val opsCenterActor = actorOf(OpsCenterClusterInfoActor.props(self))
  val graphiteClusterActor = actorOf(GraphiteClusterMetricsActor.props(self))
  val graphiteKeyspaceActor = actorOf(GraphiteKeyspaceMetricsActor.props(self))
  val userActor = actorOf(ClusterUserActor.props(self))

  var taskData = mutable.Map[ClusterGroupKey, ClusterEnv]()
  var metaResult = mutable.Map[ClusterGroupKey, Option[Metadata]]()
  var slowQueryResult = mutable.Map[ClusterGroupKey, ClusterSlowQueries]()
  var opsCenterResults = mutable.Map[ClusterGroupKey, Option[OpsCenterClusterInfo]]()
  var graphiteClusterResults = mutable.Map[ClusterGroupKey, List[MetricResult]]()
  var graphiteKeyspaceResults = mutable.Map[ClusterGroupKey, Map[String, List[MetricResult]]]()
  var usersResults = mutable.Map[ClusterGroupKey, Option[Set[String]]]()

  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Starting collecting cluster related info of $group 's ${cluster.cluster_name}")
      val taskKey = ClusterGroupKey(cluster.cluster_name, group) -> cluster
      taskData += taskKey
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, group)
      slowQueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, group)
      graphiteClusterActor ! GraphiteClusterMetricsActor.StartWorkOnCluster(cluster, group)
      userActor ! ClusterUserActor.StartWorkOnCluster(cluster, group)

    case ClusterMetadataActor.Finished(metadataResponse, cluster, group) =>
      val requestKey = ClusterGroupKey(cluster.cluster_name, group)
      metadataResponse match{
        case Left(e)=>
          log.error(s"Received failed cluster metadata of $group's ${cluster.cluster_name}. ${e.getMessage}")
          metaResult += requestKey -> None
          opsCenterResults += requestKey -> None
          graphiteKeyspaceResults += requestKey -> Map()
        case Right(metadata)=>
          metaResult += requestKey -> Some(metadata)
          log.info(s"Received cluster metadata of $requestKey")
          opsCenterActor ! OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, metadata, group)
          graphiteKeyspaceActor ! GraphiteKeyspaceMetricsActor.StartWorkOnCluster(cluster,
            metadata.getKeyspaces.asScala.toList.map(_.getName),
            requestKey)
      }
      collectResults(requestKey)

    case ClusterSlowQueryActor.Finished(slowQueries, cluster, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      slowQueryResult += clusterGroupKey -> slowQueries
      log.info(s"Received cluster slow query data of $clusterGroupKey")
      collectResults(clusterGroupKey)

    case OpsCenterClusterInfoActor.Finished(opsInfo, cluster, meta, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      opsCenterResults += clusterGroupKey -> opsInfo
      log.info(s"Received opscenter info of $clusterGroupKey")
      collectResults(clusterGroupKey)

    case GraphiteClusterMetricsActor.Finished(metricValues, cluster, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      graphiteClusterResults += clusterGroupKey -> metricValues
      log.info(s"Received graphite metrics of $clusterGroupKey")
      collectResults(clusterGroupKey)

    case GraphiteKeyspaceMetricsActor.Finished(metricValues, clusterGroupKey) =>
      graphiteKeyspaceResults += clusterGroupKey -> metricValues
      log.info(s"Received graphite keyspace metrics of $clusterGroupKey")
      collectResults(clusterGroupKey)

    case ClusterUserActor.Finished(userListResponse, cluster, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      userListResponse match {
        case Left(e) =>
          log.error(s"Received failed cluster users list of $clusterGroupKey.", e)
          usersResults += clusterGroupKey -> None
        case Right(users) =>
          log.info(s"Receuved user list of $clusterGroupKey")
          usersResults += clusterGroupKey -> Some(users)
      }
      collectResults(clusterGroupKey)
  }

  implicit class LogWaiting[K,V](wrapped: mutable.Map[K, V]){
    def getOrAction(key:K, action: =>Unit): Option[V] ={
      val result = wrapped.get(key)
      if (result.isEmpty)
        action
      result
    }
  }

  def collectResults(key: ClusterGroupKey): Unit ={
    val clusterOption = taskData.getOrAction(key,
      log.warning(s"Finished collecting all information of $key, but because of earlier failures (no task data defined), failed to construct clusterInfo")
    )
    if (clusterOption.isEmpty)
      requester ! ClusterInfoActor.WorkOnClusterFinished(null, None)
    val cluster = clusterOption.get

    val metaElement = metaResult.getOrAction(key,
      log.info(s"Still waiting for metadata of $key"))

    val slowQueryElement = slowQueryResult.getOrAction(key,
      log.info(s"Still waiting for slow query data of $key"))

    val opsInfoElement = opsCenterResults.getOrAction(key,
      log.info(s"Still waiting for opscenter info of $key"))

    val graphiteElement = graphiteClusterResults.getOrAction(key,
      log.info(s"Still waiting for graphite info of $key"))

    val usersElement = usersResults.getOrAction(key,
      log.info(s"Still waiting for users info of $key"))

    val graphiteKeyspaceElement = graphiteKeyspaceResults.getOrAction(key,
      log.info(s"Still waiting for keyspace graphite metrics of $key"))

    if (
      metaElement.isDefined &&
      slowQueryElement.isDefined &&
      opsInfoElement.isDefined &&
      graphiteElement.isDefined &&
      usersElement.isDefined &&
      graphiteKeyspaceElement.isDefined
    ) {
      if (metaElement.get.isEmpty) {
        log.warning(s"Finished collecting all information of $key, but because of earlier failures (no metadata retrieved), failed to construct clusterInfo")
        requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, None)
      } else {
        log.info(s"Finished collecting all information of $key, ")
        try {
          val metaDataValue = metaElement.get.get
          val result = new ClusterInfo(
            metaDataValue.getClusterName,
            metaDataValue.checkSchemaAgreement(),
            metaDataValue.getAllHosts.asScala.toSet,
            metaDataValue.getKeyspaces.asScala.toList,
            slowQueryElement.get,
            opsInfoElement.get,
            graphiteElement.get,
            graphiteKeyspaceElement.get,
            usersElement.get.getOrElse(Set()),
            cluster,
            key.group
            )
          requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, Some(result))
        } catch {
          case e: Throwable =>
            log.error(e, "Failed to construct clusterInfo")
            requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, None)
        }
      }
    }
  }
}
