package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.{AuthorizedGraphiteReader, MetricResult}

object GraphiteClusterMetricsActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, taskKey: ClusterActorTaskKey)
  case class Finished(graphiteResults: List[MetricResult], taskKey: ClusterActorTaskKey)
  def props(requester: ActorRef): Props = Props(new GraphiteClusterMetricsActor(requester))
}


class GraphiteClusterMetricsActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv): List[MetricResult] = {
    try {
      AuthorizedGraphiteReader.retrieveAll(
        cluster.graphiteConfig.graphite_login,
        cluster.graphiteConfig.graphite_cluster_metrics,
        cluster.graphite + (("cluster", cluster.cluster_name)))
    }catch {
      case e:Throwable =>
        log.warning(e.getMessage)
        List()
    }
  }

  override def receive: Receive = {
    case GraphiteClusterMetricsActor.StartWorkOnCluster(cluster, taskKey) =>
      log.info(s"Get graphic metrics for cluster ${cluster.cluster_name}")
      sender ! GraphiteClusterMetricsActor.Finished(process(cluster), taskKey)
  }

}
