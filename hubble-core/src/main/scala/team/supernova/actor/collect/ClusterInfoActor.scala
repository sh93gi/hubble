package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.ClusterInfo
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries}
import team.supernova.graphite.MetricResult
import team.supernova.opscenter.OpsCenterClusterInfo

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success}

object ClusterInfoActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class WorkOnClusterFinished(cluster: ClusterEnv, clusterResults: Option[ClusterInfo])
  def props(requester: ActorRef): Props = Props(new ClusterInfoActor(requester))
}

/**
  * Holds the information needed by ClusterInfoActor to know to which task a message is related
  *
  * @param clusterName the name of the cluster for which tasks are dnoe
  * @param group the group oft the cluster for which tasks are done
  */
case class ClusterActorTaskKey(clusterName: String, group: String) {
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

  var taskData = mutable.Map[ClusterActorTaskKey, ClusterEnv]()
  var metaResult = mutable.Map[ClusterActorTaskKey, Option[Metadata]]()
  var slowQueryResult = mutable.Map[ClusterActorTaskKey, ClusterSlowQueries]()
  var opsCenterResults = mutable.Map[ClusterActorTaskKey, Option[OpsCenterClusterInfo]]()
  var graphiteClusterResults = mutable.Map[ClusterActorTaskKey, List[MetricResult]]()
  var graphiteKeyspaceResults = mutable.Map[ClusterActorTaskKey, Map[String, List[MetricResult]]]()
  var usersResults = mutable.Map[ClusterActorTaskKey, Option[Set[String]]]()

  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Starting collecting cluster related info of $group 's ${cluster.cluster_name}")
      val taskKey = ClusterActorTaskKey(cluster.cluster_name, group)
      taskData += taskKey -> cluster
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, taskKey)
      slowQueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, taskKey)
      graphiteClusterActor ! GraphiteClusterMetricsActor.StartWorkOnCluster(cluster, taskKey)
      userActor ! ClusterUserActor.StartWorkOnCluster(cluster, taskKey)

    case ClusterMetadataActor.Finished(metadataResponse, taskKey) =>
      metadataResponse match{
        case Failure(e)=>
          log.error(s"Received failed cluster metadata for $taskKey. ${e.getMessage}")
          metaResult += taskKey -> None
          opsCenterResults += taskKey -> None
          graphiteKeyspaceResults += taskKey -> Map()
        case Success(metadata)=>
          metaResult += taskKey -> Some(metadata)
          log.info(s"Received cluster metadata for $taskKey")
          taskData.get(taskKey) match {
            case Some(cluster) =>
              opsCenterActor ! OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, metadata, taskKey)
              graphiteKeyspaceActor ! GraphiteKeyspaceMetricsActor.StartWorkOnCluster(cluster,
                metadata.getKeyspaces.asScala.toList.map(_.getName),
                taskKey)
            case None =>
              log.warning(s"Received cluster metadata for UNKNOWN task identified by $taskKey")
          }
      }
      collectResults(taskKey)

    case ClusterSlowQueryActor.Finished(slowQueries, taskKey) =>
      slowQueryResult += taskKey -> slowQueries
      log.info(s"Received cluster slow query data for $taskKey")
      collectResults(taskKey)

    case OpsCenterClusterInfoActor.Finished(opsInfo, taskKey) =>
      opsCenterResults += taskKey -> opsInfo
      log.info(s"Received opscenter info for $taskKey")
      collectResults(taskKey)

    case GraphiteClusterMetricsActor.Finished(metricValues, taskKey) =>
      graphiteClusterResults += taskKey -> metricValues
      log.info(s"Received graphite metrics for $taskKey")
      collectResults(taskKey)

    case GraphiteKeyspaceMetricsActor.Finished(metricValues, taskKey) =>
      graphiteKeyspaceResults += taskKey -> metricValues
      log.info(s"Received graphite keyspace metrics for $taskKey")
      collectResults(taskKey)

    case ClusterUserActor.Finished(userListResponse, taskKey) =>
      userListResponse match {
        case Failure(e) =>
          log.error(s"Received failed cluster users list for $taskKey.", e)
          usersResults += taskKey -> None
        case Success(users) =>
          log.info(s"Received user list for $taskKey")
          usersResults += taskKey -> Some(users)
      }
      collectResults(taskKey)
  }

  implicit class GetOrActionMap[K,V](wrapped: mutable.Map[K, V]){
    def getOrAction(key:K, action: =>Unit): Option[V] ={
      val result = wrapped.get(key)
      if (result.isEmpty)
        action
      result
    }
  }

  def collectResults(key: ClusterActorTaskKey): Unit ={
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
