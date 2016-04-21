package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}

object ClusterMetadataActor{
  case class StartWorkOnCluster(cluster: ClusterEnv, group: String)
  case class Finished(clusterResults: Either[Throwable, Metadata], cluster: ClusterEnv, group: String)

  def props(requester: ActorRef) : Props =
    Props(new ClusterMetadataActor(requester))
}

class ClusterMetadataActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv, group: String): Either[Throwable, Metadata] =
  {
    try {
      Right(new CassandraClusterApi(cluster).metadata())
    } catch {
      case e: Throwable => Left(e)
    }
  }

  override def receive: Receive = {
    case ClusterMetadataActor.StartWorkOnCluster(cluster, group) =>
      log.info(s"Get cluster metadata for ${cluster.cluster_name}")
      sender ! ClusterMetadataActor.Finished(process(cluster, group), cluster, group)
  }

}
