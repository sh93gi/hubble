package team.supernova.graphite

import team.supernova.validation.Check

case class MetricResult(name: String, value: Option[Double], formatter: Double=>String, source: MetricSource, checks: List[Check]){

  def formatted:Option[String] = value.map(formatter)
}
