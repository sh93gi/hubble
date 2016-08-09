package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.ClusterEnv
import team.supernova.graphite.{AuthorizedGraphiteReader, MetricResult}

object GraphiteKeyspaceMetricsActor {
  case class StartWorkOnCluster(cluster: ClusterEnv, keyspaces: List[String], key: ClusterActorTaskKey)
  case class Finished(graphiteResults: Map[String, List[MetricResult]], key: ClusterActorTaskKey)
  def props(requester: ActorRef): Props = Props(new GraphiteKeyspaceMetricsActor(requester))
}


class GraphiteKeyspaceMetricsActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, keyspaces: List[String]): Map[String, List[MetricResult]] = {
    keyspaces.map(keyspace => keyspace -> keyspace)
      .toMap.par.mapValues(keyspace => {
      try {
        val graphiteParamsMap = cluster.graphite.map(Map()+ (("keyspace", keyspace)) + (("cluster", cluster.cluster_name))++_)
        AuthorizedGraphiteReader.retrieveAll(
          cluster.graphiteConfig.graphite_login,
          cluster.graphiteConfig.graphite_keyspace_metrics,
          graphiteParamsMap
        )
      }catch {
        case e:Throwable =>
          log.warning(e.getMessage)
          List()
      }
    }).toList.toMap
  }

  override def receive: Receive = {
    case GraphiteKeyspaceMetricsActor.StartWorkOnCluster(cluster, keyspaces, key) =>
      log.info(s"Get graphic metrics for keyspaces in ${cluster.cluster_name}")
      sender ! GraphiteKeyspaceMetricsActor.Finished(process(cluster, keyspaces), key)
  }

}

