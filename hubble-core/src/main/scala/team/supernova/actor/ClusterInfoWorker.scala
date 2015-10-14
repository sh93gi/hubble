package team.supernova.actor

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core._
import team.supernova.ClusterInfo

object ClusterInfoWorker {
  case class RetrieveClusterInfo(clusterGroup: String, clusterName: String, hosts: List[String],port: Int, uname: String, pword: String, graphite_host: String,graphana_host: String, ops_hosts: String, ops_uname: String, ops_pword: String, sequence: Int)
  case class ClusterInfoRetrieved(clusterInfo: ClusterInfo)
}

class ClusterInfoWorker extends Actor with ActorLogging  {
  override def receive: Receive = {
    case ClusterInfoWorker.RetrieveClusterInfo(clusterGroup, clusterName, hosts, port, uname, pword, graphite_host, graphana_host,ops_hosts, ops_uname, ops_pword, sequence ) => {
      log.info(s"Get cluster info for $clusterName")
      //TODO get OpsCenter Info via Actor!
      //val opsCenterClusterInfo = OpsCenter.createOpsCenterClusterInfo(ops_hosts, ops_uname, ops_pword, clusterName )
      lazy val clusSes: Session = Cluster.builder().addContactPoints(hosts: _*).withCompression(ProtocolOptions.Compression.SNAPPY).withCredentials(uname, pword).withPort(port).build().connect()
      //TODO add ops Center Info!!!!! via messages!!!!!
      // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
      val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata, graphite_host, graphana_host, sequence, ops_hosts, ops_uname, ops_pword)
      clusSes.close()
      sender ! ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo)
    }
  }

}