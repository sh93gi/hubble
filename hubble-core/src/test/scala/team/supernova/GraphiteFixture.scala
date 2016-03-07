package team.supernova

import team.supernova.graphite.GraphiteApi

trait GraphiteFixture extends ClusterFixture{

  val graphiteApi = new GraphiteApi(clusterInstance.graphite)
  val graphiteUserName = system.settings.config.getString("graphite.username")
  val graphitePassword = system.settings.config.getString("graphite.password")
}
