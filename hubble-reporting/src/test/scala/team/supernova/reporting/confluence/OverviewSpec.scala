package team.supernova.reporting.confluence

import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.scalatest._

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
class OverviewSpec extends FunSpecLike {

  val config = ConfigFactory.load()
  val space = config.getString("hubble.confluence.space")

   it  ("Confluence Test gen List") {
     Overview.generateList (ConfluenceToken.getConfluenceToken (config), space )
   }

}



