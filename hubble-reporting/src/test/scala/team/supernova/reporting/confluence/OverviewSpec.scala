package team.supernova.reporting.confluence

import java.util.Properties

import org.scalatest._

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
class OverviewSpec extends FunSpecLike {

  val props: Properties = new Properties
  props.load(this.getClass.getClassLoader.getResourceAsStream("test.properties"))
  val space = props.getProperty("confluence.space")

   it  ("Confluence Test gen List") {
     Overview.generateList (ConfluenceToken.getConfluenceToken ("test.properties"), space )
   }

}



