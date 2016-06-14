package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.cassandra.{ClusterEnv, OpsCenterApi}
import team.supernova.opscenter.OpsCenterClusterInfo

object OpsCenterClusterInfoActor{
  case class StartWorkOnCluster(cluster: ClusterEnv, meta: Metadata, taskKey: ClusterActorTaskKey)
  case class Finished(opsInfo: Option[OpsCenterClusterInfo], taskKey: ClusterActorTaskKey)

  def props(requester: ActorRef) : Props =
    Props(new OpsCenterClusterInfoActor(requester))
}

class OpsCenterClusterInfoActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, meta: Metadata)=
  try{
     new OpsCenterApi(cluster).getInfo(meta)
  } catch {
    case e: Throwable=>
      log.error(e, "During OpsCenter's clusterinfo retrieval.")
      None
  }

  override def receive: Receive = {
    case OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, meta, taskKey) =>
      log.info(s"Get opscenter info for ${cluster.cluster_name}")
      sender ! OpsCenterClusterInfoActor.Finished(process(cluster, meta), taskKey)
  }

}
