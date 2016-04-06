package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.exceptions.{ReadTimeoutException, UnauthorizedException}
import team.supernova.cassandra.{CassandraSlowQueryApi, ClusterEnv, ClusterSlowQueries}

object ClusterSlowQueryActor{
  case class StartWorkOnCluster(clusterEnv: ClusterEnv, group: String)
  case class Finished(clusterResults: ClusterSlowQueries, clusterEnv: ClusterEnv, group: String)

  def props(requester: ActorRef) : Props =
    Props(new ClusterSlowQueryActor(requester))
}

class ClusterSlowQueryActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv) = {
    val clusterSlowQueries = new ClusterSlowQueries(Some(25))
    var mycounter = 0
    try{
      new CassandraSlowQueryApi(cluster).foreach(None)(q=>{
        clusterSlowQueries.add(q)
        mycounter+=1
        if (mycounter%10000==0)
          log.info(s"So far collected ${clusterSlowQueries.clusterSlow.commands.size} unique slow queries for the cluster ${cluster.cluster_name}, processed $mycounter.")
      })
    } catch {
      case e: ReadTimeoutException=>
        log.error(e, s"When collecting slow queries of ${cluster.cluster_name}")
        log.warning(s"Failed to collect all slow queries of ${cluster.cluster_name} because of a read timeout")
        clusterSlowQueries.failed(e)
      case e: UnauthorizedException=>
        log.error(e, s"When collecting slow queries of ${cluster.cluster_name}")
        clusterSlowQueries.failed(e)
      case e: Throwable=>
        log.error(e, s"When collecting slow queries of ${cluster.cluster_name}")
        clusterSlowQueries.failed(e)
    }
    log.info(s"Collected ${clusterSlowQueries.clusterSlow.commands.size} unique slow queries for the cluster ${cluster.cluster_name}, processed $mycounter.")
    clusterSlowQueries
  }

  override def receive: Receive = {
    case ClusterSlowQueryActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get cluster slow queries for ${cluster.cluster_name}")
      sender ! ClusterSlowQueryActor.Finished(process(cluster), cluster, group)
  }

}