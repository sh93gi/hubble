package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries}
import team.supernova.{ClusterInfo, OpsCenterClusterInfo}

import scala.collection.mutable

object ClusterInfoActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class WorkOnClusterFinished(clusterResults: ClusterInfo)
  def props(requester: ActorRef): Props = Props(new ClusterInfoActor(requester))
}

class ClusterInfoActor(requester: ActorRef) extends Actor with ActorLogging {
  import context._

  val metaActor = actorOf(ClusterMetadataActor.props(self))
  val slowqueryActor = actorOf(ClusterSlowQueryActor.props(self))
  val opsCenterActor = actorOf(OpsCenterClusterInfoActor.props(self))

  type ClusterName = String
  type ClusterGroup = String
  var metaResult = mutable.Map[(ClusterName, ClusterGroup), Metadata]()
  var slowQueryResult = mutable.Map[(ClusterName, ClusterGroup), ClusterSlowQueries]()
  var opsCenterResults = mutable.Map[(ClusterName, ClusterGroup), Option[OpsCenterClusterInfo]]()


  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Starting collecting cluster related info of $group 's ${cluster.cluster_name}")
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, group)
      slowqueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, group)

    case ClusterMetadataActor.Finished(metadata, cluster, group) =>
      metaResult.+=( ((cluster.cluster_name, group), metadata))
      log.info(s"Received cluster metadata of $group 's ${cluster.cluster_name}")
      opsCenterActor ! OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, metadata, group)
      collectResults(cluster, group)

    case ClusterSlowQueryActor.Finished(slowQueries, cluster, group) =>
      slowQueryResult.+=( ((cluster.cluster_name, group), slowQueries))
      log.info(s"Received cluster slow query data of $group 's ${cluster.cluster_name}")
      collectResults(cluster, group)

    case OpsCenterClusterInfoActor.Finished(opsInfo, cluster, meta, group) =>
      opsCenterResults.+=( ((cluster.cluster_name, group), opsInfo))
      log.info(s"Received opscenter info of $group 's ${cluster.cluster_name}")
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

    if (metaElement.isDefined && slowQueryElement.isDefined && opsinfoElement.isDefined){
      val result = new ClusterInfo(
        metaElement.get,
        slowQueryElement.get,
        opsinfoElement.get,
        cluster, group)
      requester ! ClusterInfoActor.WorkOnClusterFinished(result)
    }
  }
}
