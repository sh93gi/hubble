package team.supernova

import java.util.Map.Entry

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigObject, ConfigValue}
import team.supernova.actor.HubbleActor
import team.supernova.actor.HubbleActor.Start
import team.supernova.actor.collect.CassandraClusterGroup
import team.supernova.cassandra.ClusterEnv
import team.supernova.confluence.ConfluenceToken
import team.supernova.graphite.GraphiteMetric

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object HubbleApp extends App {

  val system = ActorSystem("ClusterInfo")
  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)

  startScheduleEveryDay()

  def startScheduleEveryDay() = {
    val SPACE = system.settings.config.getString("hubble.confluence.space")

    val clusters = mapConfigToCassandraClusterGroup(system.settings.config)
    val app = system.actorOf(HubbleActor.props(SPACE, TOKEN, system))
    app ! Start(clusters)
  }

  def toMap(items: ConfigObject) = {
    (for {
      entry : Entry[String, ConfigValue] <- items.entrySet().asScala
      key = entry.getKey
      value = entry.getValue.unwrapped().toString
    } yield (key, value)).toMap
  }


  def mapConfigToClusterEnv(graphite_url: String, graphite_metrics: List[GraphiteMetric], pr: Config): ClusterEnv = {
    new ClusterEnv(pr.getString("cluster_name"),
      pr.getString("graphana"),
      graphite_url,
      toMap(pr.getObject("graphite")),
      graphite_metrics,
      pr.getStringList("hosts").map(_.toString).toArray, pr.getString("ops_pword"), pr.getString("ops_uname"), pr.getString("opscenter"),
      pr.getInt("port"), pr.getString("pword"), pr.getString("uname"), pr.getInt("sequence"))
  }

  def toMetric(cf: Config):GraphiteMetric={
    new GraphiteMetric(cf.getString("url"),
      cf.getString("name"),
      {if (!cf.hasPath("func")) None else Some(cf.getString("func"))})
  }

  def mapConfigToCassandraClusterGroup(config: Config): List[CassandraClusterGroup] = {
    val graphite_url=config.getString("hubble.graphite.url_template")
    val graphite_metrics = config.getConfigList("hubble.graphite.metrics").map(toMetric).toList
    config.getConfigList("hubble.cassandra.clusters")
      .map({ p: Config => new CassandraClusterGroup(p.getString("name"), p.getConfigList("envs")
        .map(c=>mapConfigToClusterEnv(graphite_url, graphite_metrics, c)).toList)
    }).toList
  }
}
