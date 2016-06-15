package team.supernova.graphite

import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.slf4j.LoggerFactory
import team.supernova._

object AuthorizedGraphiteReader{
  def retrieveAll(graphiteLogin: GraphiteLogin, metrics:List[GraphiteMetricConfig], params:Map[String, String]):List[MetricResult]={
    metrics.map(metric =>
      (
        MetricDefinition(metric.name, metric.func, metric.format),
        new AuthorizedGraphiteReader(metric.url_template, graphiteLogin.graphite_uname, graphiteLogin.graphite_pword)
        ))
      .map { case (definition, reader) =>
        reader.retrieve(params) match {
          case (source, values) => definition.process(source, values)
        }
      }
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
