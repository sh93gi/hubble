package team.supernova.graphite

import java.io.InputStream
import java.net.URL
import java.util.Base64

import org.slf4j.LoggerFactory
import team.supernova._

object AuthorizedGraphiteReader{
  def retrieveAll(graphiteLogin: GraphiteLogin, metrics:List[GraphiteMetricConfig], paramList:List[Map[String, String]]):List[MetricResult]={
    if (paramList.isEmpty)
      return List()
    metrics.map(metric=>{
      val paramResults = paramList.map{ case (params)=>
        retrieve(
          graphiteLogin,
          metric.url_template,
          params) match{
          case (source, values) =>
            MetricDefinition(metric).process(source, values, params)
        }
      }
      paramResults.find(_.value.isDefined) match{
        case None=>paramResults.head
        case Some(definedMetric)=>definedMetric
      }
    }
    )
  }

  def retrieve(graphiteLogin: GraphiteLogin, urlTemplate: String, params:Map[String, String]):
    (MetricSource, List[List[Double]])={
     new AuthorizedGraphiteReader(urlTemplate, graphiteLogin.graphite_uname, graphiteLogin.graphite_pword)
      .retrieve(params)
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
