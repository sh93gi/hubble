package team.supernova.actor

import akka.actor.{Props, Actor, ActorLogging}
import com.datastax.driver.core.Session
import team.supernova.confluence.soap.rpc.soap.actions.Token

object ClusterInfoCollector {
  case class Start(clusterGroup: String, session: Session)
  def props(space: String, group: String, token: Token): Props = Props(new ClusterInfoCollector(space, group, token))
}

class ClusterInfoCollector(sSpace: String, sGroup: String, token: Token) extends Actor with ActorLogging {

  val group = context.system.actorOf(ClusterInfoController.props(self))
  val pages = context.system.actorOf(ConfluencePage.props(sSpace, sGroup, token))

  override def receive: Receive = {
    case ClusterInfoCollector.Start(clusterGroup, session) => {
      log.info("START: generate Confluence pages")
      group ! ClusterInfoController.RetrieveAllClusterInfo(clusterGroup, session)
    }
    case ClusterInfoController.AllClusterInfoRetrieved(allClusters) => {
      log.info(s"All ClusterInfo retrieved for group: $group")
      pages ! ConfluencePage.GenerateAll(allClusters)
    }
    case ConfluencePage.Done => {
      log.info("END: Confluence pages generated")
    }
  }
}