package team.supernova.graphite

case class GraphiteMetricConfig(url_template: String, name: String, func: Option[String], format: Option[String])
