package team.supernova.reporting.confluence

import com.typesafe.config.ConfigFactory
import org.scalatest._
import team.supernova.HubbleApp

abstract class OverviewSpecBase extends FunSpecLike {

  val config = ConfigFactory.load()
  val space = HubbleApp.mapConfigToConfluenceSpace(config)

   it  ("Confluence Test gen List") {
     Overview.generateList (ConfluenceToken.getConfluenceToken (config), space )
   }

}



