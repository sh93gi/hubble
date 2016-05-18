package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries}
import team.supernova.graphite.MetricResult
import team.supernova.{ClusterInfo, OpsCenterClusterInfo}

import scala.collection.mutable

object ClusterInfoActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class WorkOnClusterFinished(cluster: ClusterEnv, clusterResults: Option[ClusterInfo])
  def props(requester: ActorRef): Props = Props(new ClusterInfoActor(requester))
}

class ClusterInfoActor(requester: ActorRef) extends Actor with ActorLogging {
  import context._

  val metaActor = actorOf(ClusterMetadataActor.props(self))
  val slowqueryActor = actorOf(ClusterSlowQueryActor.props(self))
  val opsCenterActor = actorOf(OpsCenterClusterInfoActor.props(self))
  val graphiteActor = actorOf(GraphiteMetricActor.props(self))

  type ClusterName = String
  type ClusterGroup = String
  var metaResult = mutable.Map[(ClusterName, ClusterGroup), Option[Metadata]]()
  var slowQueryResult = mutable.Map[(ClusterName, ClusterGroup), ClusterSlowQueries]()
  var opsCenterResults = mutable.Map[(ClusterName, ClusterGroup), Option[OpsCenterClusterInfo]]()
  var graphiteResults = mutable.Map[(ClusterName, ClusterGroup), List[MetricResult]]()

  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Starting collecting cluster related info of $group 's ${cluster.cluster_name}")
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, group)
      slowqueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, group)
      graphiteActor ! GraphiteMetricActor.StartWorkOnCluster(cluster, group)

    case ClusterMetadataActor.Finished(metadataResponse, cluster, group) =>{
      metadataResponse match{
        case Left(e)=>
          log.error(s"Received failed cluster metadata of $group's ${cluster.cluster_name}. ${e.getMessage}")
          metaResult.+=( ((cluster.cluster_name, group), None))
          opsCenterResults.+=( ((cluster.cluster_name, group), None))
        case Right(metadata)=>
          metaResult.+=( ((cluster.cluster_name, group), Some(metadata)))
          log.info(s"Received cluster metadata of $group 's ${cluster.cluster_name}")
          opsCenterActor ! OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, metadata, group)
      }
      collectResults(cluster, group)
    }

    case ClusterSlowQueryActor.Finished(slowQueries, cluster, group) =>
      slowQueryResult.+=( ((cluster.cluster_name, group), slowQueries))
      log.info(s"Received cluster slow query data of $group 's ${cluster.cluster_name}")
      collectResults(cluster, group)

    case OpsCenterClusterInfoActor.Finished(opsInfo, cluster, meta, group) =>
      opsCenterResults.+=( ((cluster.cluster_name, group), opsInfo))
      log.info(s"Received opscenter info of $group 's ${cluster.cluster_name}")
      collectResults(cluster, group)

    case GraphiteMetricActor.Finished(metricValues, cluster, group) =>
      graphiteResults.+=( ((cluster.cluster_name, group), metricValues))
      log.info(s"Received graphite metrics of $group 's ${cluster.cluster_name}")
      collectResults(cluster, group)
  }

  def collectResults(cluster: ClusterEnv, group: String): Unit ={
    val metaElement = metaResult.get((cluster.cluster_name, group))
    if (!metaElement.isDefined)
      log.info(s"Still waiting for metadata of $group 's ${cluster.cluster_name}")

    val slowQueryElement = slowQueryResult.get((cluster.cluster_name, group))
    if (!slowQueryElement.isDefined)
      log.info(s"Still waiting for slow query data of $group 's ${cluster.cluster_name}")

    val opsinfoElement = opsCenterResults.get((cluster.cluster_name, group))
    if (!opsinfoElement.isDefined)
      log.info(s"Still waiting for opscenter info of $group 's ${cluster.cluster_name}")

    val graphiteElement = graphiteResults.get((cluster.cluster_name, group))
    if (!graphiteElement.isDefined)
      log.info(s"Still waiting for graphite info of $group 's ${cluster.cluster_name}")

    if (metaElement.isDefined && slowQueryElement.isDefined && opsinfoElement.isDefined && graphiteElement.isDefined){
      if (metaElement.get.isEmpty){
        log.warning(s"Finished collecting all information of $group 's ${cluster.cluster_name}, but because of earlier failures, failed to construct clusterInfo")
        requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, None)
      } else{
        log.info(s"Finished collecting all information of $group 's ${cluster.cluster_name}, ")
        try {
          val result = new ClusterInfo(
            metaElement.get.get,
            slowQueryElement.get,
            opsinfoElement.get,
            graphiteElement.get,
            cluster, group)
          requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, Some(result))
        }catch{
          case e:Throwable =>
              log.error(e,"Failed to construct clusterInfo")
              requester ! ClusterInfoActor.WorkOnClusterFinished(cluster, None)
        }
      }
    }
  }
}
