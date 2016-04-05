package team.supernova

import akka.actor.ActorSystem

trait CassandraClusterGroupFixture {
  def system: ActorSystem
  val cassandragroup = HubbleApp.mapConfigToCassandraClusterGroup(system.settings.config)

}
