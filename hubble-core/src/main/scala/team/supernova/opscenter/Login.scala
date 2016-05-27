package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}
import org.slf4j.LoggerFactory

case class Login (sessionid: String) {}

object Login {
  val log= LoggerFactory.getLogger(classOf[Login])
  def parseLogin (body: String) : Login = {
    implicit val formats = DefaultFormats
    log.info(s"Login response: $body")
    parse(body).extract[Login]
  }
}


