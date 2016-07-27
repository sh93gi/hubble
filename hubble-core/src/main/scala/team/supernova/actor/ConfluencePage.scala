package team.supernova.actor

import akka.actor.{Actor, ActorLogging, Props}
import team.supernova.confluence.{ClusterGroupHierarchy, ClusterOverallPage}
import team.supernova.confluence.soap.rpc.soap.actions.Token
import team.supernova.results.{ClusterInfo, GroupClusters}

object ConfluencePage {

  case object Done

  case class GenerateAll(clusterInfoSet: Set[ClusterInfo])

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
          try {
            val allClusters = GroupClusters(clusters.toSeq.sorted)
            ClusterGroupHierarchy.generateClusterGroupHierarchyPages(allClusters, space, groupName, token, deletePages = false)
          } catch {
            case e: Exception => log.error(e, s"Failed to create confluence pages for $groupName in $space")
          }
        }
        try {
          ClusterOverallPage.generateOverallClustersPage(clusterMap, token, space)
        } catch {
          case e: Exception => log.error(e, s"Failed to create confluence page summarizing all clusters in $space")
        }
      } catch {
        case e: Exception => log.error(e, "Failed to create confluence pages")
      } finally {
        sender ! ConfluencePage.Done
      }
  }
}
