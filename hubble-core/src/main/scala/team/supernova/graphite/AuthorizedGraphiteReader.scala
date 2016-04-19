package team.supernova.graphite

import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.slf4j.LoggerFactory
import team.supernova._
import team.supernova.cassandra.ClusterEnv

object AuthorizedGraphiteReader{
  def retrieveAll(cluster: ClusterEnv):List[MetricResult]={
    cluster.graphiteConfig.graphite_metrics.map(metric =>
      (
        MetricDefinition(metric.name, metric.func, metric.format),
        new AuthorizedGraphiteReader(metric.url_template, cluster.graphiteConfig.graphite_uname, cluster.graphiteConfig.graphite_pword)
        ))
      .map(a => {
        val graphiteResult = a._2.retrieve(cluster.graphite)
        a._1.process(graphiteResult._1, graphiteResult._2)
      })
  }
}
class AuthorizedGraphiteReader(url_template: String, graphiteUserName: String, graphitePassword: String) {
  val log = LoggerFactory.getLogger(classOf[AuthorizedGraphiteReader])

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

  def retrieve(templateArgs: Map[String, String]): (MetricSource, List[List[Double]]) = {
    val url = new StringTemplate(url_template+"&rawData=true").fillWith(templateArgs)
    val csv = using(authorizedInputStream(url)) { isr => {
      scala.io.Source.fromInputStream(isr).getLines().toList
    }
    }
    log.info(s"Retrieved ${csv.size} metric series from $url")
    val values = csv.map(line=>line.split('|').last
      .split(",").filterNot(_.equals("None")).map(_.trim.toDouble).toList)
      .filterNot(_.isEmpty)
    (MetricSource(url.replaceAllLiterally("&rawData=true", "")), values)
  }


}
