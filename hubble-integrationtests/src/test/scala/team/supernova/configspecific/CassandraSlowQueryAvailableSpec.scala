package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.CassandraSlowQueryAvailableSpecBase

class CassandraSlowQueryAvailableSpec extends CassandraSlowQueryAvailableSpecBase{

  /**
    * A ClusterEnv which has a dse_perf.node_slow_log table
    *
    * @return
    */
  override def clusterEnvWithSlowQueries: ClusterEnv = cassandragroup.head.envs.head // one with dse_perf keyspace
}
