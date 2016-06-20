package team.supernova.graphite

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import team.supernova.validation.Check

class MetricDefinition(val name: String, aggregator: Iterable[Double]=>Double, formatter: Double=>String,
                       checks: List[(Double, Map[String, String])=>Check]){

  def process(metricSource: MetricSource, values: List[List[Double]], templateArgs: Map[String, String]): MetricResult ={
    val metricVal =
      if (values.isEmpty)
        None
      else
        Some(aggregator(values.map(aggregator(_))))
    MetricResult(name, metricVal, formatter, metricSource,
      metricVal match {
        case Some(value) => checks.map(_(value, templateArgs))
        case None => List()
      })
  }
}

object MetricDefinition{
  val log = LoggerFactory.getLogger(classOf[MetricDefinition])

  def apply(config: GraphiteMetricConfig): MetricDefinition = {
    new MetricDefinition(config.name, getAggregateFuncImplementation(config.func), getToStringFuncImplmentation(config.format),
      config.checks.map(getCheckCreationImplementation) )
  }

  def getCheckCreationImplementation(config: GraphiteMetricCheckConfig) : (Double, Map[String, String] )=> Check =
    (value:Double, templateArgs : Map[String, String])=> {
      val comparison = getComparisonFuncImplementation(config.check)
      val details = new StringTemplate(config.details).fillWith(templateArgs)
      Check(config.name, details, comparison(value, config.threshold), config.severity)
    }

  def getComparisonFuncImplementation(funcDef:String): (Double, Double)=>Boolean = {
    funcDef.toLowerCase match{
      case "gt" => _ > _
      case "lt" => _ < _
      case "eq" => _ == _
      case "neq" => _ != _
    }
  }
  def getAggregateFuncImplementation(funcDef:Option[String]):Iterable[Double]=>Double = {
    funcDef.map(_.toLowerCase) match{
      case Some("avg") => a=>a.sum/a.size
      case Some("sum") => a=>a.sum
      case Some("min") => a=>a.min
      case Some("max") => a=>a.max
      case Some("delta") => a=>a.last-a.head
      case None => a=>a.last
      case other =>
        log.warn(s"Failed to determine aggregate function. Only one of avg, sum, min, max, delta are possible. Did not find '$other'")
        throw new MatchError(other)
    }
  }

  def getToStringFuncImplmentation(funcDef: Option[String]):Double=>String={
    funcDef.map(_.toLowerCase) match{
      case Some("bytecount") => a=>FileUtils.byteCountToDisplaySize(Math.round(a))
      case None=>_.toString
      case other =>
        log.warn(s"Failed to determine metric formatting function. Either don't provide 'bytecount' or none. Did not find '$other'")
        throw new MatchError(other)
    }
  }

}

