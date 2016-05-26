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
  val graphiteActor = actorOf(GraphiteMetricActor.props(self))
  val userActor = actorOf(ClusterUserActor.props(self))


  var metaResult = mutable.Map[ClusterGroupKey, Option[Metadata]]()
  var slowQueryResult = mutable.Map[ClusterGroupKey, ClusterSlowQueries]()
  var opsCenterResults = mutable.Map[ClusterGroupKey, Option[OpsCenterClusterInfo]]()
  var graphiteResults = mutable.Map[ClusterGroupKey, List[MetricResult]]()
  var usersResults = mutable.Map[ClusterGroupKey, Option[Set[String]]]()

  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Starting collecting cluster related info of $group 's ${cluster.cluster_name}")
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, group)
      slowQueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, group)
      graphiteActor ! GraphiteMetricActor.StartWorkOnCluster(cluster, group)
      userActor ! ClusterUserActor.StartWorkOnCluster(cluster, group)

    case ClusterMetadataActor.Finished(metadataResponse, cluster, group) =>{
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      metadataResponse match{
        case Left(e)=>
          log.error(s"Received failed cluster metadata of $group's ${cluster.cluster_name}. ${e.getMessage}")
          metaResult += clusterGroupKey -> None
          opsCenterResults += clusterGroupKey -> None
        case Right(metadata)=>
          metaResult += clusterGroupKey -> Some(metadata)
          log.info(s"Received cluster metadata of $clusterGroupKey")
          opsCenterActor ! OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, metadata, group)
      }
      collectResults(clusterGroupKey, cluster)
    }

    case ClusterSlowQueryActor.Finished(slowQueries, cluster, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      slowQueryResult += clusterGroupKey -> slowQueries
      log.info(s"Received cluster slow query data of $clusterGroupKey")
      collectResults(clusterGroupKey, cluster)

    case OpsCenterClusterInfoActor.Finished(opsInfo, cluster, meta, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      opsCenterResults += clusterGroupKey -> opsInfo
      log.info(s"Received opscenter info of $clusterGroupKey")
      collectResults(clusterGroupKey, cluster)

    case GraphiteMetricActor.Finished(metricValues, cluster, group) =>
      val clusterGroupKey = ClusterGroupKey(cluster.cluster_name, group)
      graphiteResults += clusterGroupKey -> metricValues
      log.info(s"Received graphite metrics of $clusterGroupKey")
      collectResults(clusterGroupKey, cluster)

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
      collectResults(clusterGroupKey, cluster)
  }

  def collectResults(key: ClusterGroupKey, cluster: ClusterEnv): Unit ={
    val metaElement = metaResult.get(key)
    if (metaElement.isEmpty)
      log.info(s"Still waiting for metadata of $key")

    val slowQueryElement = slowQueryResult.get(key)
    if (slowQueryElement.isEmpty)
      log.info(s"Still waiting for slow query data of $key")

    val opsInfoElement = opsCenterResults.get(key)
    if (opsInfoElement.isEmpty)
      log.info(s"Still waiting for opscenter info of $key")

    val graphiteElement = graphiteResults.get(key)
    if (graphiteElement.isEmpty)
      log.info(s"Still waiting for graphite info of $key")

    val usersElement = usersResults.get(key)
    if (usersElement.isEmpty)
      log.info(s"Still waiting for users info of $key")

    if (
      metaElement.isDefined &&
      slowQueryElement.isDefined &&
      opsInfoElement.isDefined &&
      graphiteElement.isDefined &&
      usersElement.isDefined
    ) {
      if (metaElement.get.isEmpty) {
        log.warning(s"Finished collecting all information of $key, but because of earlier failures, failed to construct clusterInfo")
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
            usersElement.get.getOrElse(Set()),
            cluster,
            key.group)
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
