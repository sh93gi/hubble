package team.supernova.actor.collect

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.OpsCenterClusterInfo
import team.supernova.cassandra.{OpsCenterApi, ClusterEnv}

object OpsCenterClusterInfoActor{
  case class StartWorkOnCluster(cluster: ClusterEnv, meta: Metadata, group: String)
  case class Finished(opsInfo: Option[OpsCenterClusterInfo], cluster: ClusterEnv, meta: Metadata, group: String)

  def props(requester: ActorRef) : Props =
    Props(new OpsCenterClusterInfoActor(requester))
}

class OpsCenterClusterInfoActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, meta: Metadata)=
     new OpsCenterApi(cluster).getInfo(meta)

  override def receive: Receive = {
    case OpsCenterClusterInfoActor.StartWorkOnCluster(cluster, meta, group) =>
      log.info(s"Get opscenter info for ${cluster.cluster_name}")
      sender ! OpsCenterClusterInfoActor.Finished(process(cluster, meta), cluster, meta, group)
  }

}
