package team.supernova.actor

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import team.supernova.cassandra.ClusterEnv
import team.supernova.ClusterInfo

import scala.collection.SortedSet

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
object ClusterInfoController {
  case class RetrieveAllClusterInfo(clusters: List[CassandraClusterGroup])
  case class AllClusterInfoRetrieved(clusterInfoSet: SortedSet[ClusterInfo])
  def props(requester: ActorRef): Props = Props(new ClusterInfoController(requester))

}

case class CassandraClusterGroup(name: String, envs: List[ClusterEnv])


class ClusterInfoController(requester: ActorRef) extends Actor with ActorLogging  {

  var counter = 0
  var clusterInfoList: SortedSet[ClusterInfo] = SortedSet.empty
  val worker = context.actorOf(RoundRobinPool(5).props(Props[ClusterInfoWorker]), "clientInfoWorker")

  def processClusterInfoPerRow (clusterEnv: ClusterEnv, clusterGroup: String) = {
    // ask for new cluster info
    counter += 1
    log.info(s"Querying cassandra - " + clusterEnv.cluster_name)
    worker ! ClusterInfoWorker.RetrieveClusterInfo(clusterEnv, clusterGroup)
  }

  override def receive: Receive = {
    case ClusterInfoController.RetrieveAllClusterInfo(cluster) => {
      cluster.foreach(cluster => {
        log.info(s"Get all clusterInfo for - ${cluster.name}")
        cluster.envs.foldLeft() {
          (a, row) => processClusterInfoPerRow(row, cluster.name)
        }
      })
    }

    case ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo) if counter == 1 => {
      log.info(s"Message received from worker = $counter")
      counter -= 1
      clusterInfoList = clusterInfoList ++ SortedSet(clusterInfo)
      log.info(s"All rows processed. Total:  ${clusterInfoList.size}")

      requester ! ClusterInfoController.AllClusterInfoRetrieved(clusterInfoList)
    }

    case ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo) => {
      log.info(s"Message received from worker = $counter")
      counter -= 1
      clusterInfoList = clusterInfoList ++ SortedSet(clusterInfo)
    }
  }
}





