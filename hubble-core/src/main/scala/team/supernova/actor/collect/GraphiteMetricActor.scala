package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.{AuthorizedGraphiteReader, MetricResult}

object GraphiteMetricActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class Finished(graphiteResults: List[MetricResult], cluster: ClusterEnv, group: String)
  def props(requester: ActorRef): Props = Props(new GraphiteMetricActor(requester))
}


class GraphiteMetricActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv): List[MetricResult] = {
    try {
      AuthorizedGraphiteReader.retrieveAll(cluster)
    }catch {
      case e:Throwable =>
        log.warning(e.getMessage)
        List()
    }
  }

  override def receive: Receive = {
    case GraphiteMetricActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get graphic metrics for ${cluster.cluster_name}")
      sender ! GraphiteMetricActor.Finished(process(cluster), cluster, group)
  }

}
