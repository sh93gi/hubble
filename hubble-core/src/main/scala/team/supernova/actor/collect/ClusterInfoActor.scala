package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.ClusterInfo
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries}

import scala.collection.mutable

object ClusterInfoActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class WorkOnClusterFinished(clusterResults: ClusterInfo)
  def props(requester: ActorRef): Props = Props(new ClusterInfoActor(requester))
}

class ClusterInfoActor(requester: ActorRef) extends Actor with ActorLogging {
  import context._

  val metaActor = actorOf(ClusterMetadataActor.props(self))
  val slowqueryActor = actorOf(ClusterSlowQueryActor.props(self))

  type ClusterName = String
  type ClusterGroup = String
  var metaResult = mutable.Map[(ClusterName, ClusterGroup), Metadata]()
  var slowQueryResult = mutable.Map[(ClusterName, ClusterGroup), ClusterSlowQueries]()


  override def receive: Receive = {
    case ClusterInfoActor.StartWorkOnCluster(cluster, group) =>
      log.info("Starting cluster info data")
      metaActor ! ClusterMetadataActor.StartWorkOnCluster(cluster, group)
      slowqueryActor ! ClusterSlowQueryActor.StartWorkOnCluster(cluster, group)

    case ClusterMetadataActor.Finished(resultSet, cluster, group) =>
      metaResult.+=( ((cluster.cluster_name, group), resultSet))
      log.info("Received cluster metadata")
      collectResults(cluster, group)

    case ClusterSlowQueryActor.Finished(resultSet, cluster, group) =>
      slowQueryResult.+=( ((cluster.cluster_name, group), resultSet))
      log.info("Received cluster slow query data")
      collectResults(cluster, group)
  }

  def collectResults(cluster: ClusterEnv, group: String): Unit ={
    val metaElement = metaResult.get((cluster.cluster_name, group))
    val slowQueryElement = slowQueryResult.get((cluster.cluster_name, group))
    if (metaElement.isDefined && slowQueryElement.isDefined){
      requester ! ClusterInfoActor.WorkOnClusterFinished(new ClusterInfo(metaElement.get, slowQueryElement.get, cluster, group))
    }
  }
}
