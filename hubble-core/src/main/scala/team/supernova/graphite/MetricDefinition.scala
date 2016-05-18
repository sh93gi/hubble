package team.supernova.graphite

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

class MetricDefinition(val name: String, aggregator: Iterable[Double]=>Double, formatter: Double=>String){

  def process(metricSource: MetricSource, values: List[List[Double]]): MetricResult ={
    val metricVal =
      if (values.isEmpty)
        None
      else
        Some(aggregator(values.map(aggregator(_))))
    MetricResult(name, metricVal, formatter, metricSource)
  }
}

object MetricDefinition{
  val log = LoggerFactory.getLogger(classOf[MetricDefinition])

  def apply(name: String, func: Option[String], format: Option[String]): MetricDefinition = {
    new MetricDefinition(name, getAggregateFuncImplementation(func), getToStringFuncImplmentation(format))
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

