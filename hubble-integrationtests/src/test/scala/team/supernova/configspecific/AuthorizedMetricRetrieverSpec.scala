package team.supernova.configspecific

import team.supernova.cassandra.ClusterEnv
import team.supernova.testsuites.AuthorizedMetricRetrieverSpecBase

class AuthorizedMetricRetrieverSpec extends AuthorizedMetricRetrieverSpecBase{
  override def clusterEnvWithHttpAuthorizedGraphite: ClusterEnv = cassandragroup.head.envs.last
}
