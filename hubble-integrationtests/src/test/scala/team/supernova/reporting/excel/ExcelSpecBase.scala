package team.supernova.reporting.excel

import com.typesafe.config.ConfigFactory
import org.scalatest._
import team.supernova.HubbleApp
import team.supernova.reporting.confluence.{ConfluenceToken, Overview}

abstract class ExcelSpecBase extends FunSpecLike {

  val config = ConfigFactory.load()
  val space = HubbleApp.mapConfigToConfluenceSpace(config)

  it  ("Test Create excel sheet") {
    Excel.createSheet (Overview.getKeyspacesInfoFromManualPage (ConfluenceToken.getConfluenceToken (config), space ))
  }

}
