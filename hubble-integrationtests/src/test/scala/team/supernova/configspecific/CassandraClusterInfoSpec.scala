package team.supernova.configspecific

import team.supernova.cassandra.{CassandraClusterGroupFixture, ClusterEnv}
import team.supernova.testsuites.CassandraClusterInfoSpecBase

class CassandraClusterInfoSpec extends CassandraClusterInfoSpecBase with CassandraClusterGroupFixture{

  val clusterInstance: ClusterEnv = cassandragroup.head.envs.last // small cluster, such that clusterinfo is fast
}
