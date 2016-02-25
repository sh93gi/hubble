package team.supernova.cassandra

import com.datastax.driver.core._
import team.supernova.ClusterInfo

class CassandraClusterApi(cluster: ClusterEnv) {

  def clusterInfo(group: String): ClusterInfo = {
    lazy val clusSes: Session = new ClusterEnvConnector(cluster).connect()
    try{
      //TODO add ops Center Info!!!!! via messages!!!!!
      // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
      val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata, cluster, group)
      println("clusterInfo: " + clusterInfo)
      return clusterInfo
    } finally{
      clusSes.close()
    }
  }
}
