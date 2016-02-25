package team.supernova.actor

import akka.actor.{Actor, ActorLogging}
import team.supernova.ClusterInfo
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}

object ClusterInfoWorker {
  case class RetrieveClusterInfo(cluster: ClusterEnv, group: String)
  case class ClusterInfoRetrieved(clusterInfo: ClusterInfo)
}

class ClusterInfoWorker extends Actor with ActorLogging  {
  override def receive: Receive = {
    case ClusterInfoWorker.RetrieveClusterInfo(cluster, group) => {
      log.info(s"Get cluster info for ${cluster.cluster_name}")
      //TODO get OpsCenter Info via Actor!
      //val opsCenterClusterInfo = OpsCenter.createOpsCenterClusterInfo(ops_hosts, ops_uname, ops_pword, clusterName )

      val clusterInfo = new CassandraClusterApi(cluster).clusterInfo(group)
      sender ! ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo)
    }
  }

}