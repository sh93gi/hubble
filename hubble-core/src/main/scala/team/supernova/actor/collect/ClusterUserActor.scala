package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}

import scala.util.Try

object ClusterUserActor {

  case class StartWorkOnCluster(cluster: ClusterEnv, taskKey: ClusterActorTaskKey)

  case class Finished(clusterResults: Try[Set[String]], taskKey: ClusterActorTaskKey)

  def props(requester: ActorRef): Props = Props(new ClusterUserActor(requester))

}

class ClusterUserActor(requester: ActorRef) extends Actor with ActorLogging {

  def process(cluster: ClusterEnv): Try[Set[String]] = {
    Try(new CassandraClusterApi(cluster).users())
  }

  override def receive: Receive = {
    case ClusterUserActor.StartWorkOnCluster(cluster, taskKey) =>
      log.info(s"Get cluster users list for : ${cluster.cluster_name}")
      sender ! ClusterUserActor.Finished(process(cluster), taskKey)
  }
}
