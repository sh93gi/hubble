package team.supernova.graphite

class GraphiteApi(graphiteUrl: String) {

  def clusterGraphUrl(clusterName: String, width: Int =400, height: Int =250): String =
    s"https://${graphiteUrl}/render?from=-2hours&until=now&width=${width}&height=${height}&target=LLDS.Cassandra.${clusterName}.requests.mean&target=LLDS.Cassandra.${clusterName}.requests.max&target=LLDS.Cassandra.${clusterName}.requests.p99&_uniq=0.8728725090622902"
}
