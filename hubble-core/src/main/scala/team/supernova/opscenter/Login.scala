package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

case class Login (sessionid: String) {}

object Login {
  def parseLogin (body: String) : Login = {
    implicit val formats = DefaultFormats
    parse(body).extract[Login]
  }
}


