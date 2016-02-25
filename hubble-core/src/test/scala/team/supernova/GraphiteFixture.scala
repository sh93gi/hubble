package team.supernova

import team.supernova.graphite.GraphiteApi

trait GraphiteFixture extends ClusterFixture{

  val graphiteApi = new GraphiteApi(clusterInstance.graphite)
}
