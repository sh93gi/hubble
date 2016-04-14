package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, Props, ActorRef}
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.AuthorizedMetricRetriever

object GraphiteMetricActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class Finished(graphiteResults: List[(String, Option[Double])], cluster: ClusterEnv, group: String)
  def props(requester: ActorRef): Props = Props(new GraphiteMetricActor(requester))
}


class GraphiteMetricActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv): List[(String, Option[Double])] = {
    try {
      AuthorizedMetricRetriever.retrieveAll(cluster)
    }catch {
      case e:Exception =>
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
