package team.supernova.graphite

case class GraphiteConfig(graphite_plot: GraphitePlotConfig,
                          graphite_metrics: List[GraphiteMetricConfig],
                          graphite_uname: String,
                          graphite_pword: String)

