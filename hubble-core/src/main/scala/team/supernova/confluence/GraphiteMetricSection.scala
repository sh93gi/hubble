package team.supernova.confluence

import team.supernova.graphite.MetricResult

import scala.xml.NodeSeq

case class MetricXml(metric: MetricResult){
  def nameNodes():NodeSeq={
    scala.xml.Text(metric.name)
  }

  def resultNodes():NodeSeq={
    val sourceUrl = scala.xml.Utility.escape(metric.source.url)
    val resultText = metric.formatted.getOrElse("N/A")
    <a href={sourceUrl}>
      {scala.xml.Text(resultText)}
    </a>
  }
}


class GraphiteMetricTable(metricNames: List[String]){
  /**
    * Sorted unique metric names
    */
  val headerNames = metricNames.toSet.toList.sorted

  /**
    * Create th node sequence (not including tr), to easily add metrics to an existing table
    * @return nodeseq to insert within a tr to add metric header columns to a table
    */
  def headerCells() : NodeSeq={
    headerNames.map(name=> <th>{scala.xml.Text(name)}</th>).toSeq
  }

  /**
    * Create td node sequence (not including tr), to easily add metric values to an existing table
    * @return nodeseq to insert within a tr to add metric value columns to a table
    */
  def contentCells(metrics: List[MetricResult]) : NodeSeq={
    // Create string(cell) representation of metric results
    // N/A for metrics which were tried, but no result
    val metricMap = metrics.map(a=>(a.name, a)).toMap.mapValues(result=> MetricXml(result).resultNodes()
    )
    // if one of the total metrics not found in this set, empty cell
    headerNames.map(header=>
      <td>{metricMap.getOrElse(header, NodeSeq.Empty)}</td>
      ).toSeq
  }

}

object GraphiteMetricSection {
  def singleMetricTable(metrics: List[MetricResult]): NodeSeq={
    if (metrics.isEmpty)
      return NodeSeq.Empty
    <h1>Cluster Metrics</h1>
    <table>
      <tbody><tr><th>Metric Name</th><th>Metric Value</th></tr>
        {metrics.sortBy(_.name).map(MetricXml).map(result=>
        {
          <tr><td>{result.nameNodes()}</td><td>{ result.resultNodes()}</td></tr>
        }).toSeq }
      </tbody>
    </table>
  }

  def combinedMetricTable(multimetrics  : Map[String, List[MetricResult]]) : NodeSeq = {
    //Are there any metrics at all?
    if (!multimetrics.values.flatten.map(_.value).exists(_.isDefined))
      return NodeSeq.Empty
    // All metrics, to distinguish between N/A (requested, but not found) and not requested
    val metricTable = new GraphiteMetricTable(multimetrics.values.flatten.map(_.name).toList)
    <h1>Cluster Metrics Summary</h1>
    <p>
      <table>
        <tbody><tr><th>Cluster</th>{metricTable.headerCells()}</tr>
          {multimetrics.toList.sortBy(_._1).map(kv=>{
            val clusterName = kv._1
            val clusterMetrics = kv._2
            <tr><td>{scala.xml.Utility.escape(clusterName)}</td>{metricTable.contentCells(clusterMetrics)}</tr>
          }).toSeq}
        </tbody>
      </table>
    </p>
  }


}
