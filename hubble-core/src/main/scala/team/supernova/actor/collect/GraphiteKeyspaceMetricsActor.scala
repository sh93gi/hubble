package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.{AuthorizedGraphiteReader, MetricResult}

object GraphiteKeyspaceMetricsActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, keyspaces: List[String], key: ClusterGroupKey)
  case class Finished(graphiteResults: Map[String, List[MetricResult]], key: ClusterGroupKey)
  def props(requester: ActorRef): Props = Props(new GraphiteKeyspaceMetricsActor(requester))
}


class GraphiteKeyspaceMetricsActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, keyspaces: List[String]): Map[String, List[MetricResult]] = {
    keyspaces.map(keyspace => keyspace -> keyspace)
      .toMap.mapValues(keyspace => {
      try {
        AuthorizedGraphiteReader.retrieveAll(
          cluster.graphiteConfig.graphite_login,
          cluster.graphiteConfig.graphite_keyspace_metrics,
          cluster.graphite.+(("keyspace", keyspace)))
      }catch {
        case e:Throwable =>
          log.warning(e.getMessage)
          List()
      }
    })
  }

  override def receive: Receive = {
    case GraphiteKeyspaceMetricsActor.StartWorkOnCluster(cluster, keyspaces, key) =>
      log.info(s"Get graphic metrics for keyspaces in ${cluster.cluster_name}")
      sender ! GraphiteKeyspaceMetricsActor.Finished(process(cluster, keyspaces), key)
  }

}

