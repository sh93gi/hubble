package team.supernova.confluence

import akka.actor.ActorSystem
import team.supernova.HubbleApp

trait ConfluenceFixture {
  def system: ActorSystem

  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)
  val SPACE = HubbleApp.mapConfigToConfluenceSpace(system.settings.config)
}
