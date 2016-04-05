package team.supernova

import akka.actor.ActorSystem

trait CassandraClusterGroupFixture {
  def system: ActorSystem
  val cassandragroup = ClusterInfoApp.mapConfigToCassandraClusterGroup(system.settings.config)

}
