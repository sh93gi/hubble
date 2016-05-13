package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.GraphiteSpecBase

class GraphiteSpec extends GraphiteSpecBase{

  /**
    * A clusterInstance which is used to fill in the graphite template
    * Example value: cassandragroup.head.envs.last
    */
  override def clusterInstanceWithGraphiteService: ClusterEnv = cassandragroup.head.envs.last

}
