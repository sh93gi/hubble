package team.supernova.actor

import akka.actor._
import team.supernova.confluence.soap.rpc.soap.actions.Token

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ClusterInfoCollector {

  case class Start(clusters: List[CassandraClusterGroup])

  def props(space: String, token: Token, system: ActorSystem): Props = Props(new ClusterInfoCollector(space, token, system))
}

class ClusterInfoCollector(sSpace: String, token: Token, systemValue: ActorSystem) extends Actor with ActorLogging {
  import context._

  val group = actorOf(ClusterInfoController.props(self))
  val pages = actorOf(ConfluencePage.props(sSpace, token))

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
      val result = Future {
        system.shutdown()
        system.awaitTermination()
      }
      result onComplete {
        case Success(r) => if (systemValue.isTerminated) sys.exit()
        case Failure(t) => println("An error has occured: " + t.getMessage)
      }
    }
  }
}