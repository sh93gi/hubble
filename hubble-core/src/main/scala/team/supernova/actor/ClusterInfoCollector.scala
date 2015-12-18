package team.supernova.actor

import akka.actor.{Props, Actor, ActorLogging}
import com.datastax.driver.core.Session
import team.supernova.confluence.soap.rpc.soap.actions.Token

object ClusterInfoCollector {
  case class Start(clusters: List[CassandraClusterGroup])
  def props(space: String, token: Token): Props = Props(new ClusterInfoCollector(space, token))
}

class ClusterInfoCollector(sSpace: String, token: Token) extends Actor with ActorLogging {

  val group = context.system.actorOf(ClusterInfoController.props(self))
  val pages = context.system.actorOf(ConfluencePage.props(sSpace, token))

  override def receive: Receive = {
    case ClusterInfoCollector.Start(clusters) => {
      log.info("START: generate Confluence pages")
      group ! ClusterInfoController.RetrieveAllClusterInfo(clusters)
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