package team.supernova.cassandra

import com.datastax.driver.core.Metadata
import org.slf4j.LoggerFactory
import team.supernova.using

class CassandraClusterApi(cluster: ClusterEnv) {
  val log = LoggerFactory.getLogger(classOf[CassandraClusterApi])

  def metadata(): Metadata = {
    using(new ClusterEnvConnector(cluster).connect()){
      clusSes=>{
        val metadata = clusSes.getCluster.getMetadata
        log.info(s"cluster metadata: $metadata")
        metadata
      }
    }
  }
}
