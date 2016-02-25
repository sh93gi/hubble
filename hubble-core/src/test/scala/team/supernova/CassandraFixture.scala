package team.supernova

import com.datastax.driver.core.Session
import team.supernova.cassandra.ClusterEnvConnector

trait CassandraFixture extends ClusterFixture{

  def newSession(): Session = new ClusterEnvConnector(clusterInstance).connect()
}