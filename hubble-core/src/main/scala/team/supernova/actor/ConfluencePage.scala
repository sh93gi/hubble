package team.supernova.actor

import akka.actor.{Props, Actor, ActorLogging}
import team.supernova.GroupClusters
import team.supernova.confluence.GenerateCassandraConfluencePages
import team.supernova.confluence.soap.rpc.soap.actions.Token

object ConfluencePage {
  case object Done
  case class  GenerateAll(allClusters: GroupClusters)

  def props(space: String, group: String, token: Token): Props = Props(new ConfluencePage(space, group, token))
}

class ConfluencePage(space: String, group: String, token: Token) extends Actor with ActorLogging {
  override def receive: Receive = {
    case ConfluencePage.GenerateAll(allClusters) => {
      log.info("Start generating confluence pages")
      // TODO make reactive
      GenerateCassandraConfluencePages.generateAllConfluencePages(allClusters, space, group, token, false)
      sender ! ConfluencePage.Done
    }
  }
}
