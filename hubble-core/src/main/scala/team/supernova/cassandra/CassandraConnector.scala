package team.supernova.cassandra

import com.datastax.driver.core._

trait CassandraConnector{
  def connect():Session
}

class ClusterEnvConnector(val cluster: ClusterEnv) extends CassandraConnector{

  def connect():Session = {
    Cluster.builder().
      addContactPoints(cluster.hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withCredentials(cluster.uname, cluster.pword).
      withPort(cluster.port).build().connect()
  }
}
