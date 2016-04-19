package team.supernova

import java.util.Map.Entry

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigObject, ConfigValue}
import team.supernova.actor.HubbleActor
import team.supernova.actor.HubbleActor.Start
import team.supernova.actor.collect.CassandraClusterGroup
import team.supernova.cassandra.ClusterEnv
import team.supernova.confluence.ConfluenceToken
import team.supernova.graphite.GraphiteMetricConfig

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


  def mapConfigToClusterEnv(graphiteConfig: GraphiteConfig, pr: Config): ClusterEnv = {
    new ClusterEnv(pr.getString("cluster_name"),
      pr.getString("graphana"),
      graphiteConfig,
      toMap(pr.getObject("graphite")),
      pr.getStringList("hosts").map(_.toString).toArray, pr.getString("ops_pword"), pr.getString("ops_uname"), pr.getString("opscenter"),
      pr.getInt("port"), pr.getString("pword"), pr.getString("uname"), pr.getInt("sequence"))
  }

  def toMetric(cf: Config):GraphiteMetricConfig={
    new GraphiteMetricConfig(cf.getString("url"),
      cf.getString("name"),
      {if (!cf.hasPath("func")) None else Some(cf.getString("func"))},
      {if (!cf.hasPath("format")) None else Some(cf.getString("format"))}
    )
  }

  case class GraphiteConfig(graphite_template: String,
                            graphite_metrics: List[GraphiteMetricConfig],
                            graphite_uname: String,
                            graphite_pword: String)

  def mapConfigToGraphite(config: Config): GraphiteConfig = {
    val graphite_url=config.getString("hubble.graphite.url_template")
    val graphite_uname=config.getString("hubble.graphite.username")
    val graphite_pword=config.getString("hubble.graphite.password")
    val graphite_metrics = config.getConfigList("hubble.graphite.metrics").map(toMetric).toList
    GraphiteConfig(graphite_url, graphite_metrics, graphite_uname, graphite_pword)
  }

  def mapConfigToCassandraClusterGroup(config: Config): List[CassandraClusterGroup] = {
    val graphite = mapConfigToGraphite(config)
    config.getConfigList("hubble.cassandra.clusters")
      .map({ p: Config => new CassandraClusterGroup(p.getString("name"), p.getConfigList("envs")
        .map(cf=>mapConfigToClusterEnv(graphite, cf)).toList)
    }).toList
  }
}
