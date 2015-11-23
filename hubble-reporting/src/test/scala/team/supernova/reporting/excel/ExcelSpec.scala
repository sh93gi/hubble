package team.supernova.reporting.excel

import java.util.Properties

import org.scalatest._
import team.supernova.reporting.confluence.{ConfluenceToken, Overview}

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */

class ExcelSpec extends FunSpecLike {

  val props: Properties = new Properties
  props.load(this.getClass.getClassLoader.getResourceAsStream("test.properties"))
  val space = props.getProperty("confluence.space")

  it  ("Test Create excel sheet") {
    Excel.createSheet (Overview.getKeyspacesInfoFromManualPage (ConfluenceToken.getConfluenceToken ("test.properties"), space ))
  }

}
