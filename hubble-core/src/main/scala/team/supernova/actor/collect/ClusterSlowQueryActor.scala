package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.{CassandraSlowQueryApi, ClusterEnv, ClusterSlowQueries}

object ClusterSlowQueryActor{
  case class StartWorkOnCluster(clusterEnv: ClusterEnv, group: String)
  case class Finished(clusterResults: ClusterSlowQueries, clusterEnv: ClusterEnv, group: String)

  def props(requester: ActorRef) : Props =
    Props(new ClusterSlowQueryActor(requester))
}

class ClusterSlowQueryActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv) = {
    val clusterSlowQueries = new ClusterSlowQueries(Some(5))
    new CassandraSlowQueryApi(cluster).foreach(clusterSlowQueries.add)
    clusterSlowQueries
  }

  override def receive: Receive = {
    case ClusterSlowQueryActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get cluster slow queries for ${cluster.cluster_name}")
      sender ! ClusterSlowQueryActor.Finished(process(cluster), cluster, group)
  }

}