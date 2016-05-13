package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.CassandraSlowQueryUnavailableSpecBase

class CassandraSlowQueryUnavailableSpec extends CassandraSlowQueryUnavailableSpecBase {

  /**
    * A ClusterEnv which has a dse_perf.node_slow_log table
    *
    * @return
    */
  override def clusterEnvWithSlowQueries: ClusterEnv = cassandragroup.head.envs.last // one without dse_perf keyspace
}
