package team.supernova.cassandra

import akka.actor.ActorSystem
import team.supernova.HubbleApp

trait CassandraClusterGroupFixture {

  def system: ActorSystem
  val cassandragroup = HubbleApp.mapConfigToCassandraClusterGroup(system.settings.config)

}
