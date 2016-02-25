package team.supernova

import akka.actor.ActorSystem
import com.datastax.driver.core.Session
import team.supernova.cassandra.ClusterEnvConnector


trait CassandraTestBase {
  def system: ActorSystem

  val cassandragroup = ClusterInfoApp.mapConfigToCassandraClusterGroup(system.settings.config)
  val clusterInstance = cassandragroup.last.envs.last // llds2 test, which is smaller than llds1 sbx

  def newSession(): Session = new ClusterEnvConnector(clusterInstance).connect()
}