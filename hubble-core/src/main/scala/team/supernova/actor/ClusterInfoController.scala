package team.supernova.actor

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.datastax.driver.core._
import team.supernova.{ClusterInfo, GroupClusters}

import scala.collection.SortedSet

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
object ClusterInfoController {
  case class RetrieveAllClusterInfo(clusterGroup: String, session: Session)
  case class AllClusterInfoRetrieved(allClusters: GroupClusters)
  def props(requester: ActorRef): Props = Props(new ClusterInfoController(requester))

}

class ClusterInfoController(requester: ActorRef) extends Actor with ActorLogging  {

  var counter = 0
  var clusterInfoList: SortedSet[ClusterInfo] = SortedSet.empty
  val worker = context.actorOf(RoundRobinPool(5).props(Props[ClusterInfoWorker]), "clientInfoWorker")

  def processClusterInfoPerRow (row: Row, clusterGroup: String) = {
    val cluster_name = row.getString("cluster_name")
    val sequence = row.getInt("sequence")

    //get OpsCenter details
    val ops_uname = row.getString("ops_uname")
    val ops_pword = row.getString("ops_pword")
    val ops_hosts = row.getString("opscenter")

    //cluster config
    val uname = row.getString("uname")
    val pword = row.getString("pword")
    val hosts: List[String] = row.getString("hosts").split(",").toList
    val port = row.getInt("port")

    //graphite
    val graphite_host = row.getString("graphite")
    val graphana_host = row.getString("graphana")

    // ask for new cluster info
    counter += 1
    worker ! ClusterInfoWorker.RetrieveClusterInfo(clusterGroup, cluster_name, hosts, port, uname, pword, graphite_host, graphana_host, ops_hosts,ops_uname, ops_pword, sequence)
  }

  override def receive: Receive = {
    case ClusterInfoController.RetrieveAllClusterInfo(clusterGroup, session) => {
      log.info(s"Get all clusterInfo for - $clusterGroup")
      import scala.collection.JavaConversions._
      log.info(s"Querying cassandra - " + session.getCluster.getMetadata.getClusterName + " " + session.getLoggedKeyspace )
      val clusterRes = session.execute(new SimpleStatement(s"select * from cluster where group='$clusterGroup'"))
      val clusterList= clusterRes.foldLeft() {
        (a, row) =>  processClusterInfoPerRow(row, clusterGroup)
      }
    }

    case ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo) if counter == 1 => {
      log.info(s"Message received from worker = $counter")
      counter -= 1
      clusterInfoList = clusterInfoList ++ SortedSet(clusterInfo)
      log.info(s"All rows processed. Total:  ${clusterInfoList.size}")
      val allClusters = GroupClusters(clusterInfoList)
      requester ! ClusterInfoController.AllClusterInfoRetrieved(allClusters)
    }

    case ClusterInfoWorker.ClusterInfoRetrieved(clusterInfo) => {
      log.info(s"Message received from worker = $counter")
      counter -= 1
      clusterInfoList = clusterInfoList ++ SortedSet(clusterInfo)
    }
  }
}





