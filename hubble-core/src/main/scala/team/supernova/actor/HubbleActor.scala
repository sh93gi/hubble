package team.supernova.actor

import akka.actor._
import team.supernova.actor.collect.{CassandraClusterGroup, ClusterInfoWorkerBatch}
import team.supernova.confluence.soap.rpc.soap.actions.Token

import scala.concurrent.Future
import scala.util.{Failure, Success}

object HubbleActor {

  case class Start(clusterGroups: List[CassandraClusterGroup])

  def props(space: String, token: Token, system: ActorSystem): Props = Props(new HubbleActor(space, token, system))
}

class HubbleActor(sSpace: String, token: Token, systemValue: ActorSystem) extends Actor with ActorLogging {
  import context._

  val clusterInfo = actorOf(ClusterInfoWorkerBatch.props(self))
  val pages = actorOf(ConfluencePage.props(sSpace, token))

  override def receive: Receive = {
    case HubbleActor.Start(clusterGroups) => {
      log.info("START: generate Confluence pages")
      clusterInfo ! ClusterInfoWorkerBatch.StartWorkOnAllClusterGroups(clusterGroups)
    }
    case ClusterInfoWorkerBatch.Finished(allClustersInfo) => {
      log.info(s"All ClusterInfo retrieved for group: $clusterInfo")
      pages ! ConfluencePage.GenerateAll(allClustersInfo)
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