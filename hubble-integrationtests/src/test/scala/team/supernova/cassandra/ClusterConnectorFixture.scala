package team.supernova.cassandra

import com.datastax.driver.core.Session

/**
  * Provides session factory method, requires a cluster instance to connect to
  */
trait ClusterConnectorFixture {
  def clusterInstance: ClusterEnv

  def newSession(): Session = new ClusterEnvConnector(clusterInstance).connect()
}