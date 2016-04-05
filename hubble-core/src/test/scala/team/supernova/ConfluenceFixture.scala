package team.supernova

import akka.actor.ActorSystem
import team.supernova.confluence.ConfluenceToken

trait ConfluenceFixture {
  def system: ActorSystem

  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)
  val SPACE = system.settings.config.getString("hubble.confluence.space")
}
