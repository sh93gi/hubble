package team.supernova.graphite

import akka.actor.ActorSystem
import team.supernova.cassandra.ClusterEnv

trait GraphiteFixture {
  def system: ActorSystem
  def clusterInstance: ClusterEnv

  def StringTemplate() = new StringTemplate(system.settings.config.getString("hubble.graphite.url_template"))
  val graphiteUserName = system.settings.config.getString("hubble.graphite.username")
  val graphitePassword = system.settings.config.getString("hubble.graphite.password")
}
