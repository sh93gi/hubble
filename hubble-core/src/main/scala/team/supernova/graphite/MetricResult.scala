package team.supernova.graphite

case class MetricResult(name: String, value: Option[Double], formatter: Double=>String, source: MetricSource){

  def formatted:Option[String] = value.map(formatter)
}
