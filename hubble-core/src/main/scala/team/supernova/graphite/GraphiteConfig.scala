package team.supernova.graphite

case class GraphiteConfig(graphite_plot: GraphitePlotConfig,
                          graphite_cluster_metrics: List[GraphiteMetricConfig],
                          graphite_keyspace_metrics: List[GraphiteMetricConfig],
                          graphite_login: GraphiteLogin
                         )

