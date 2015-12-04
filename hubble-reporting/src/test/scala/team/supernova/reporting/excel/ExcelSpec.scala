package team.supernova.reporting.excel

import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.scalatest._
import team.supernova.reporting.confluence.{ConfluenceToken, Overview}
/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */

class ExcelSpec extends FunSpecLike {

  val config = ConfigFactory.load()
  val space = config.getString("hubble.confluence.space")
  it  ("Test Create excel sheet") {
    Excel.createSheet (Overview.getKeyspacesInfoFromManualPage (ConfluenceToken.getConfluenceToken (config), space ))
  }

}
