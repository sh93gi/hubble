package team.supernova.actor

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core._
import team.supernova.ClusterInfo

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
      lazy val clusSes: Session = Cluster.builder().addContactPoints(cluster.hosts: _*).withCompression(ProtocolOptions.Compression.SNAPPY).withCredentials(cluster.uname, cluster.pword).withPort(cluster.port).build().connect()
      //TODO add ops Center Info!!!!! via messages!!!!!
      // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
      val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata, cluster, group)
      println("clusterInfo: " + clusterInfo)
      clusSes.close()
      sender ! ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo)
    }
  }

}