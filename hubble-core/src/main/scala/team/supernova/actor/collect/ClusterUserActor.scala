package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}

import scala.util.{Failure, Success, Try}

object ClusterUserActor {

  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)

  case class Finished(clusterResults: Either[Throwable, Set[String]], cluster: ClusterEnv, group: String)

  def props(requester: ActorRef): Props = Props(new ClusterUserActor(requester))

}

class ClusterUserActor(requester: ActorRef) extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, group: String): Either[Throwable, Set[String]] = {
    Try(new CassandraClusterApi(cluster).users()) match {
      case Success(users) => Right(users)
      case Failure(f) => Left(f)
    }
  }

  override def receive: Receive = {
    case ClusterUserActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get cluster users list for : ${cluster.cluster_name}")
      sender ! ClusterUserActor.Finished(process(cluster, group), cluster, group)
  }
}
