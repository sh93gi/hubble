package team.supernova.graphite

case class GraphiteMetricCheckConfig(name: String, details: String, check: String, threshold: Double, severity: String)
