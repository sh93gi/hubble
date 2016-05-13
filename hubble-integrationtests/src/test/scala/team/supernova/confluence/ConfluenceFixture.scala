package team.supernova.confluence

import akka.actor.ActorSystem

trait ConfluenceFixture {
  def system: ActorSystem

  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)
  val SPACE = system.settings.config.getString("hubble.confluence.space")
}
