package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.OpsCenterInfoSpecBase

class OpsCenterInfoSpec extends OpsCenterInfoSpecBase {
  override def clusterEnvWithOpsCenter: ClusterEnv = cassandragroup.head.envs.last // small cluster, such that clusterinfo is fast(er)
}
