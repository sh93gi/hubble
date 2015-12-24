package team.supernova

import akka.actor.ActorSystem
import com.datastax.driver.core.{Cluster, ProtocolOptions, Session}


trait TestCassandraCluster {
  def system: ActorSystem

  val cassandragroup = ClusterInfoApp.mapConfigToCassandraClusterGroup(system.settings.config)
  val clusterInstance = cassandragroup.head.envs.head

  lazy val session: Session =
    Cluster.builder().
      addContactPoints(clusterInstance.hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(clusterInstance.port).
      withCredentials(clusterInstance.uname, clusterInstance.pword).
      build().
      connect()
}