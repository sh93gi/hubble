package team.supernova

import akka.actor.ActorSystem
import com.datastax.driver.core.{Cluster, ProtocolOptions, Session}
import team.supernova.actor.ClusterInfoCollector
import team.supernova.confluence.ConfluenceToken
import scala.concurrent.duration._

object ClusterInfoApp extends App {

  val system = ActorSystem("ClusterInfo")
  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)

  startScheduleEveryDay()

  def startScheduleEveryDay() = {
    import system.dispatcher
    val GROUP = system.settings.config.getString("confluence.group")
    val SPACE = system.settings.config.getString("confluence.space")
    val app = system.actorOf(ClusterInfoCollector.props(SPACE, GROUP, TOKEN))
    system.scheduler.schedule(0 milliseconds, 24 hours, app, ClusterInfoCollector.Start(GROUP, cassandraSession))
  }

  def cassandraSession: Session = {
    val username = system.settings.config.getString("hubble.cassandra.username")
    val password = system.settings.config.getString("hubble.cassandra.password")
    val port = system.settings.config.getInt("hubble.cassandra.port")
    val keyspace = system.settings.config.getString("hubble.cassandra.keyspace")
    val hosts = system.settings.config.getString("hubble.cassandra.hosts").split(",").toList
    Cluster.builder().
      addContactPoints(hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(port).
      withCredentials(username, password).
      build().
      connect(keyspace)
  }
}
