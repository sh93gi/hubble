package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Metadata
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}

import scala.util.Try

object ClusterMetadataActor{
  case class StartWorkOnCluster(cluster: ClusterEnv, taskKey: ClusterActorTaskKey)
  case class Finished(clusterResults: Try[Metadata], taskKey: ClusterActorTaskKey)

  def props(requester: ActorRef) : Props =
    Props(new ClusterMetadataActor(requester))
}

class ClusterMetadataActor(requester: ActorRef)  extends Actor with ActorLogging {

  def process(cluster: ClusterEnv): Try[Metadata] =
  {
    Try( new CassandraClusterApi(cluster).metadata())
  }

  override def receive: Receive = {
    case ClusterMetadataActor.StartWorkOnCluster(cluster, taskKey) =>
      log.info(s"Get cluster metadata for ${cluster.cluster_name}")
      sender ! ClusterMetadataActor.Finished(process(cluster), taskKey)
  }

}
