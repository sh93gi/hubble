package team.supernova

import akka.actor.ActorSystem
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.GraphiteApi

trait GraphiteFixture {
  def system: ActorSystem
  def clusterInstance: ClusterEnv

  def graphiteApi() = new GraphiteApi(clusterInstance.graphite)
  val graphiteUserName = system.settings.config.getString("graphite.username")
  val graphitePassword = system.settings.config.getString("graphite.password")
}
