package team.supernova.actor

import akka.actor.{Props, Actor, ActorLogging}
import team.supernova.{ClusterInfo, GroupClusters}
import team.supernova.confluence.GenerateCassandraConfluencePages
import team.supernova.confluence.soap.rpc.soap.actions.Token

import scala.collection.SortedSet

object ConfluencePage {
  case object Done
  case class  GenerateAll(clusterInfoSet: SortedSet[ClusterInfo])

  def props(space: String, token: Token): Props = Props(new ConfluencePage(space, token))
}

class ConfluencePage(space: String, token: Token) extends Actor with ActorLogging {
  override def receive: Receive = {
    case ConfluencePage.GenerateAll(clusterInfoSet) => {
      log.info("Start generating confluence pages")
      // TODO create separate actor instead of looping over groups
      val clusterMap = clusterInfoSet.groupBy(f => f.group)
      clusterMap.foreach(
        map =>
        {
          val allClusters = GroupClusters(map._2)
          GenerateCassandraConfluencePages.generateGroupConfluencePages(allClusters, space, map._1, token, false)
        }
      )
      sender ! ConfluencePage.Done
    }
  }
}
