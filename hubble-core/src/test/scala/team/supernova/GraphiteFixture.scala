package team.supernova

import akka.actor.ActorSystem
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.StringTemplate

trait GraphiteFixture {
  def system: ActorSystem
  def clusterInstance: ClusterEnv

  def StringTemplate() = new StringTemplate(system.settings.config.getString("hubble.graphite.url_template"))
  val graphiteUserName = system.settings.config.getString("hubble.graphite.username")
  val graphitePassword = system.settings.config.getString("hubble.graphite.password")
}
