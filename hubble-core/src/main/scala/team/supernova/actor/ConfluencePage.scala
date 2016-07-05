package team.supernova.actor

import akka.actor.{Actor, ActorLogging, Props}
import team.supernova.confluence.{ClusterOverallPage, ClusterGroupHierarchy}
import team.supernova.confluence.soap.rpc.soap.actions.Token
import team.supernova.{ClusterInfo, GroupClusters}

object ConfluencePage {
  case object Done
  case class  GenerateAll(clusterInfoSet: Set[ClusterInfo])

  def props(space: String, token: Token): Props = Props(new ConfluencePage(space, token))
}

class ConfluencePage(space: String, token: Token) extends Actor with ActorLogging {
  override def receive: Receive = {
    case ConfluencePage.GenerateAll(clusterInfoSet) =>
      try {
        log.info("Start generating confluence pages")
        // TODO create separate actor instead of looping over groups
        val clusterMap = clusterInfoSet.groupBy(f => f.group)
        clusterMap.foreach { case (groupName, clusters) =>
          val allClusters = GroupClusters(clusters.toSeq.sorted)
          ClusterGroupHierarchy.generateClusterGroupHierarchyPages(allClusters, space, groupName, token, deletePages = false)
        }

        ClusterOverallPage.generateOverallClustersPage(clusterMap,token, space)

      }catch{
        case e:Exception => log.error(e, "Failed to create confluence pages")
      }finally{
        sender ! ConfluencePage.Done
      }
  }
}
