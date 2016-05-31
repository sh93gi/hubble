package team.supernova.cassandra

import com.datastax.driver.core.Metadata
import org.slf4j.LoggerFactory
import team.supernova.using

import scala.collection.JavaConversions._

class CassandraClusterApi(cluster: ClusterEnv) {
  val log = LoggerFactory.getLogger(classOf[CassandraClusterApi])

  def metadata(): Metadata = {
    using(new ClusterEnvConnector(cluster).connect()) {
      clusSes => {
        val metadata = clusSes.getCluster.getMetadata
        log.info(s"cluster metadata: $metadata")
        metadata
      }
    }
  }

  def users(): Set[String] = {
    using(new ClusterEnvConnector(cluster).connect()) {
      session =>
        val users = session.execute("LIST USERS;")
        users.all().map( user => user.getString(0)).toSet
    }
  }
}
