package team.supernova

import akka.actor.ActorSystem
import com.datastax.driver.core.{Cluster, ProtocolOptions, Session}
import com.typesafe.config.Config
import team.supernova.actor.{CassandraClusterGroup, ClusterEnv, ClusterInfoCollector}
import team.supernova.confluence.ConfluenceToken

import scala.collection.JavaConversions._
import scala.concurrent.duration._

object ClusterInfoApp extends App {

  val system = ActorSystem("ClusterInfo")
  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)

  startScheduleEveryDay()

  def startScheduleEveryDay() = {
    import system.dispatcher
    val SPACE = system.settings.config.getString("hubble.confluence.space")

    val clusters = mapConfigToCassandraClusterGroup(system.settings.config)
    val app = system.actorOf(ClusterInfoCollector.props(SPACE, TOKEN))
    ClusterInfoCollector.Start(clusters)
  }

  def mapConfigToClusterEnv(pr: Config): ClusterEnv = {
    new ClusterEnv(pr.getString("cluster_name"),
      pr.getString("graphana"),
      pr.getString("graphite"),
      pr.getStringList("hosts").map(_.toString).toArray, pr.getString("ops_pword"), pr.getString("ops_uname"), pr.getString("opscenter"),
      pr.getInt("port"), pr.getString("pword"), pr.getString("uname"), pr.getInt("sequence"))
  }

  def mapConfigToCassandraClusterGroup(config: Config): List[CassandraClusterGroup] = {
    config.getConfigList("hubble.cassandra.clusters")
      .map({ p: Config => new CassandraClusterGroup(p.getString("name"), p.getConfigList("envs")
      map mapConfigToClusterEnv toList)
    }).toList
  }
}
