package team.supernova

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigObject}
import team.supernova.actor.HubbleActor
import team.supernova.actor.HubbleActor.Start
import team.supernova.actor.collect.CassandraClusterGroup
import team.supernova.cassandra.ClusterEnv
import team.supernova.confluence.ConfluenceToken
import team.supernova.graphite._
import team.supernova.users.UserNameValidator
import team.supernova.validation.Severity

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.matching.Regex

object HubbleApp extends App {


  val system = ActorSystem("ClusterInfo")
  val TOKEN = ConfluenceToken.getConfluenceToken(system.settings.config)

  startScheduleEveryDay()

  def startScheduleEveryDay() = {
    val space = mapConfigToConfluenceSpace(system.settings.config)
    val clusters = mapConfigToCassandraClusterGroup(system.settings.config)
    val app = system.actorOf(HubbleActor.props(space, TOKEN, system))
    app ! Start(clusters)
  }

  def toMap(items: ConfigObject) = {
    (for ((key,value) <- items.asScala) yield (key, value.unwrapped().toString)).toMap
  }

  def mapConfigToUserNameValidator(config: Config) : UserNameValidator= {
    val userNameSuffixes: Set[String] = config.getStringList("hubble.cassandra.username_suffixes").asScala.toSet
    val userNameRegex: Regex = config.getString("hubble.cassandra.username_regex").r
    UserNameValidator(userNameRegex, userNameSuffixes)
  }
  def mapConfigToConfluenceSpace(config: Config) = config.getString("hubble.confluence.space")

  def mapConfigToClusterEnv(graphiteConfig: GraphiteConfig, userchecks: UserNameValidator, pr: Config): ClusterEnv = {
    new ClusterEnv(
      pr.getString("cluster_name"),
      pr.getString("graphana"),
      graphiteConfig,
      pr.getObjectList("graphite").map(toMap).toList,
      pr.getStringList("hosts").asScala.toArray[String],
      pr.getString("ops_pword"),
      pr.getString("ops_uname"),
      pr.getString("opscenter"),
      pr.getInt("port"),
      pr.getString("pword"),
      pr.getString("uname"),
      pr.getInt("sequence"),
      userchecks
    )
  }

  def toMetricCheck(severity: String, cf: Config): GraphiteMetricCheckConfig = {
    new GraphiteMetricCheckConfig(
      cf.getString("name"),
      cf.getString("details"),
      cf.getString("comparison"),
      cf.getDouble("threshold"),
      severity)
  }

  def toMetric(cf: Config): GraphiteMetricConfig = {
    new GraphiteMetricConfig(
      cf.getString("url"),
      cf.getString("name"),
      if (!cf.hasPath("func")) None else Some(cf.getString("func")),
      if (!cf.hasPath("format")) None else Some(cf.getString("format")),
      (if (!cf.hasPath("warnings")) List() else cf.getConfigList("warnings").map(toMetricCheck(Severity.WARNING, _)).toList) ++
        (if (!cf.hasPath("errors")) List() else cf.getConfigList("errors").map(toMetricCheck(Severity.ERROR, _)).toList)
    )
  }

  def toGraphitePlotConfig(cf: Config): GraphitePlotConfig = {
    new GraphitePlotConfig(cf.getString("header"), cf.getString("url_template"))
  }


  def mapConfigToGraphite(config: Config): GraphiteConfig = {
    val graphite_plot = toGraphitePlotConfig(config.getConfig("hubble.graphite.plot"))
    val graphite_uname = config.getString("hubble.graphite.username")
    val graphite_pword = config.getString("hubble.graphite.password")
    val cluster_metrics = config.getConfigList("hubble.graphite.cluster_metrics").map(toMetric).toList
    val keyspace_metrics = config.getConfigList("hubble.graphite.keyspace_metrics").map(toMetric).toList
    GraphiteConfig(graphite_plot, cluster_metrics, keyspace_metrics, GraphiteLogin(graphite_uname, graphite_pword))
  }

  def mapConfigToCassandraClusterGroup(config: Config): List[CassandraClusterGroup] = {
    val graphite = mapConfigToGraphite(config)
    val userChecks = mapConfigToUserNameValidator(config)
    config.getConfigList("hubble.cassandra.clusters")
      .map({ p: Config =>
        new CassandraClusterGroup(
          p.getString("name"),
          p.getConfigList("envs").map(cf => mapConfigToClusterEnv(graphite, userChecks, cf)).toList
        )
      }).toList
  }
}
