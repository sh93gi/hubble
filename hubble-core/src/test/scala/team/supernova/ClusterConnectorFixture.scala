package team.supernova

import com.datastax.driver.core.Session
import team.supernova.cassandra.{ClusterEnv, ClusterEnvConnector}

/**
  * Provides session factory method, requires a cluster instance to connect to
  */
trait ClusterConnectorFixture {
  def clusterInstance: ClusterEnv

  def newSession(): Session = new ClusterEnvConnector(clusterInstance).connect()
}