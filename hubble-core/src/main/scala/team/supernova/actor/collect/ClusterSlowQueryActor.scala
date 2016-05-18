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
    var myCounter = 0
    try{
      new CassandraSlowQueryApi(cluster).foreach(q=>{
        clusterSlowQueries.add(q)
        myCounter+=1
        if (myCounter%10000==0)
          log.info(s"So far collected ${clusterSlowQueries.clusterSlow.commands.size} unique slow queries for the cluster ${cluster.cluster_name}, processed $myCounter.")
      })
    } catch {
      case e: ReadTimeoutException=>
        log.warning(s"Failed to collect all slow queries of ${cluster.cluster_name} because of a read timeout. ${e.getMessage}")
        clusterSlowQueries.failed(e)
      case e: UnauthorizedException=>
        log.error(s"Unauthorized exception when collecting slow queries of ${cluster.cluster_name}. ${e.getMessage}")
        clusterSlowQueries.failed(e)
      case e: Throwable=>
        log.error(e, s"Unanticipated exception when collecting slow queries of ${cluster.cluster_name}")
        clusterSlowQueries.failed(e)
    }
    log.info(s"Collected ${clusterSlowQueries.clusterSlow.commands.size} unique slow queries for the cluster ${cluster.cluster_name}, processed $myCounter.")
    clusterSlowQueries
  }

  override def receive: Receive = {
    case ClusterSlowQueryActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get cluster slow queries for ${cluster.cluster_name}")
      sender ! ClusterSlowQueryActor.Finished(process(cluster), cluster, group)
  }

}