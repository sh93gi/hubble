package team.supernova.actor.collect

import akka.actor.{ActorRef, Props}
import team.supernova.ClusterInfo
import team.supernova.cassandra.ClusterEnv

case class CassandraClusterGroup(name: String, envs: List[ClusterEnv])

object ClusterInfoWorkerBatch{
  case class StartWorkOnAllClusterGroups(clusterGroups: List[CassandraClusterGroup])
  case class Finished(clusterResults: Set[ClusterInfo])

  def props(requester: ActorRef) : Props =
    Props(new ClusterInfoWorkerBatch(requester))
}

class ClusterInfoWorkerBatch(requester: ActorRef)  extends ClusterWorkerBatch[ClusterInfo] {

  def newActor() = ClusterInfoActor.props(self)

  override def receive: Receive = {
    case ClusterInfoWorkerBatch.StartWorkOnAllClusterGroups(cluster) =>
      runOnAll(cluster)

    case ClusterInfoActor.WorkOnClusterFinished(clusterResult: ClusterInfo) =>
      received(clusterResult)
  }

  def finished(clusterBatchResults: Set[ClusterInfo]) = {
    log.info(s"All rows processed. Total:  ${clusterResults.size}")
    requester ! ClusterInfoWorkerBatch.Finished(clusterResults)
  }

  override def message(clusterEnv: ClusterEnv, clusterGroup: String): Any =
    ClusterInfoActor.StartWorkOnCluster(clusterEnv, clusterGroup)
}