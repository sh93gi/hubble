package team.supernova

import akka.actor.ActorSystem

trait ClusterFixture {
  def system: ActorSystem
  val cassandragroup = ClusterInfoApp.mapConfigToCassandraClusterGroup(system.settings.config)
  val clusterInstance = cassandragroup.last.envs.last // llds2 test, which is smaller than llds1 sbx

}
