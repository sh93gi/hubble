package team.supernova.actor.collect

import akka.actor.{ActorRef, Props}
import team.supernova.cassandra.ClusterEnv
import team.supernova.results.ClusterInfo

import scala.collection.mutable

case class CassandraClusterGroup(name: String, envs: List[ClusterEnv])

object ClusterInfoWorkerBatch{
  case class StartWorkOnAllClusterGroups(clusterGroups: List[CassandraClusterGroup])
  case class Finished(clusterResults: Set[ClusterInfo])

  def props(requester: ActorRef) : Props =
    Props(new ClusterInfoWorkerBatch(requester))
}

class ClusterInfoWorkerBatch(requester: ActorRef)  extends ClusterWorkerBatch[Option[ClusterInfo]] {

  def newActor() = ClusterInfoActor.props(self)
  val receivedNames = mutable.ArrayBuffer[String]()
  val failedNames = mutable.ArrayBuffer[String]()

  override def receive: Receive = {
    case ClusterInfoWorkerBatch.StartWorkOnAllClusterGroups(cluster) =>
      runOnAll(cluster)

    case ClusterInfoActor.WorkOnClusterFinished(cluster, clusterResult: Option[ClusterInfo]) =>
      if(clusterResult.isDefined) {
        log.info(s"Received successfully constructed clusterinfo about ${cluster.cluster_name}, already received ${receivedNames.mkString(",")}")
      }else{
        log.info(s"Received failed to be constructed clusterinfo about ${cluster.cluster_name}. Already received ${receivedNames.mkString(",")}")
        failedNames += cluster.cluster_name
      }
      receivedNames += cluster.cluster_name
      received(clusterResult)
  }

  def finished(clusterBatchResults: Set[Option[ClusterInfo]]) = {
    log.info(s"All clusters processed, total: ${clusterResults.size}, failed to get complete results from ${failedNames.mkString("," )}")
    requester ! ClusterInfoWorkerBatch.Finished(clusterResults.filter(_.isDefined).map(_.get))
  }

  override def message(clusterEnv: ClusterEnv, clusterGroup: String): Any =
    ClusterInfoActor.StartWorkOnCluster(clusterEnv, clusterGroup)
}