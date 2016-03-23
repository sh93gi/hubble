package team.supernova.cassandra

import com.datastax.driver.core.Metadata
import org.slf4j.LoggerFactory
import team.supernova.{ClusterInfo, using}

class CassandraClusterApi(cluster: ClusterEnv) {
  val log = LoggerFactory.getLogger(classOf[CassandraClusterApi])

  def metadata(): Metadata = {
    using(new ClusterEnvConnector(cluster).connect()){
      clusSes=>{
        //TODO add ops Center Info!!!!! via messages!!!!!
        // val clusterInfo = ClusterInfo(clusSes.getCluster.getMetadata,  opsCenterClusterInfo, graphite_host, graphana_host, sequence)
        val clusterMeta = clusSes.getCluster.getMetadata
        log.info(s"clusterInfo: $clusterMeta")
        clusterMeta
      }
    }
  }
}
