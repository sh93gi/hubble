package team.supernova.graphite

import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.slf4j.LoggerFactory
import team.supernova._
import team.supernova.cassandra.ClusterEnv

object AuthorizedMetricRetriever{
  def retrieveAll(cluster: ClusterEnv)={
    cluster.graphiteConfig.graphite_metrics.map(metric =>
      (metric.name, new AuthorizedMetricRetriever(metric.url_template, metric.func, cluster.graphiteConfig.graphite_uname, cluster.graphiteConfig.graphite_pword)))
      .map(a => (a._1, a._2.retrieve(cluster.graphite)))
  }
}

class AuthorizedMetricRetriever(url_template: String, func: Option[String], graphiteUserName: String, graphitePassword: String) {
  val log = LoggerFactory.getLogger(classOf[AuthorizedMetricRetriever])
  val aggregateFunc = getAggregateFuncImplementation(func)

  def authorizedInputStream(url: String ): InputStream ={
    val name = graphiteUserName
    val password = graphitePassword
    val authString = name + ":" + password
    val authEncBytes = Base64.getEncoder.encode(authString.getBytes())
    val authStringEnc = new String(authEncBytes)

    val url_ref = new URL(url)
    val urlConnection = url_ref.openConnection()
    urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc)
    val is = urlConnection.getInputStream
    is
  }

  def getAggregateFuncImplementation(funcDef:Option[String]):Iterable[Double]=>Double = {
    funcDef.map(_.toLowerCase) match{
      case Some("avg") => a=>a.sum/a.size
      case Some("sum") => a=>a.sum
      case Some("min") => a=>a.min
      case Some("max") => a=>a.max
      case None => a=>a.last
    }
  }

  def retrieve(templateArgs: Map[String, String]): Option[Double] = {
    val url = new StringTemplate(url_template+"&rawData=true").fillWith(templateArgs)
    val csv = using(authorizedInputStream(url)) { isr => {
      scala.io.Source.fromInputStream(isr).getLines().toList
    }
    }
    log.info(s"Retrieved ${csv.size} metric series from $url")
    val values = csv.map(line=>line.split('|').last
      .split(",").filterNot(_.equals("None")).map(_.trim.toDouble))
      .filterNot(_.isEmpty)
    if (csv.isEmpty)
      None
    else
      Some(aggregateFunc(values.map(aggregateFunc(_))))
  }


}
