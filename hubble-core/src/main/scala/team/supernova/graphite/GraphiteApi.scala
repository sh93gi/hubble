package team.supernova.graphite

class GraphiteApi(graphiteUrl: String) {

  def clusterGraphUrl(clusterName: String, width: Int =400, height: Int =250): String = {
    val clusterInGraphite = clusterName.toLowerCase
    s"https://${graphiteUrl}/render?from=-2hours&until=now&width=${width}&height=${height}&target=groupByNode(movingAverage(nonNegativeDerivative(llds.cassandra.${clusterInGraphite}.cluster.*.*.org.apache.cassandra.metrics.ClientRequest.Read.Latency.count),%202),%204,%20%27sum%27)"
  }
}
