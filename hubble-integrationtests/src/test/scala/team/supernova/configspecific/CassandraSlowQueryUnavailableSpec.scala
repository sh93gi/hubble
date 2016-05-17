package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.CassandraSlowQueryUnavailableSpecBase

class CassandraSlowQueryUnavailableSpec extends CassandraSlowQueryUnavailableSpecBase {

  /**
    * A ClusterEnv which does NOT have a dse_perf.node_slow_log table
    */
  override def clusterEnvWithSlowQueries: ClusterEnv = cassandragroup.last.envs.last
}
