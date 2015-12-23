package team.supernova

import akka.actor.ActorSystem
import com.datastax.driver.core.{Cluster, ProtocolOptions, Session}


trait TestCassandraCluster {
  def system: ActorSystem

  val username = system.settings.config.getString("hubble.cassandra.username")
  val password = system.settings.config.getString("hubble.cassandra.password")
  val port = system.settings.config.getString("hubble.cassandra.port").toInt
  val keyspace = system.settings.config.getString("hubble.cassandra.keyspace")
  val hosts = system.settings.config.getString("hubble.cassandra.hosts").split(",").toList

  lazy val session: Session =
    Cluster.builder().
      addContactPoints(hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(port).
      withCredentials(username, password).
      build().
      connect(keyspace)
}