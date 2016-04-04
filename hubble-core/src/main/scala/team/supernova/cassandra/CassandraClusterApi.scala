package team.supernova.cassandra

import org.slf4j.LoggerFactory
import team.supernova.{ClusterInfo, using}

class CassandraClusterApi(cluster: ClusterEnv) {
  val log = LoggerFactory.getLogger(classOf[CassandraClusterApi])

  def clusterInfo(group: String): ClusterInfo = {
    using(new ClusterEnvConnector(cluster).connect()){
      clusSes=>{
        //TODO add ops Center Info!!!!! via messages!!!!!
        // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
        val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata, cluster, group)
        log.info(s"clusterInfo: $clusterInfo")
        return clusterInfo
      }
    }
  }
}
