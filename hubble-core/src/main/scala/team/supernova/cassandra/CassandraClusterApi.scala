package team.supernova.cassandra

import team.supernova.ClusterInfo
import team.supernova.using

class CassandraClusterApi(cluster: ClusterEnv) {
  def clusterInfo(group: String): ClusterInfo = {
    using(new ClusterEnvConnector(cluster).connect()){
      clusSes=>{
        //TODO add ops Center Info!!!!! via messages!!!!!
        // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
        val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata, cluster, group)
        println("clusterInfo: " + clusterInfo)
        return clusterInfo
      }
    }
  }
}
