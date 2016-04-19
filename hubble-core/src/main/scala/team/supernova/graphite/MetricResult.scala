package team.supernova.graphite

case class MetricResult(val name: String, value: Option[Double], formatter: Double=>String, val source: MetricSource){

  def formatted:Option[String] = value.map(formatter)
}
