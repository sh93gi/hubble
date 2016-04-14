package team.supernova.confluence

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

class GraphiteMetricTable(metricNames: List[String]){
  val headerNames = metricNames.sorted

  def headerCells() : NodeSeq={
    scala.xml.Unparsed( headerNames.mkString("<th>","</th><th>","</th>"))
  }

  def contentCells(metrics: List[(String, Option[Double])]) : NodeSeq={
    // Create string(cell) representation of metric results
    val metricMap = metrics.toMap.mapValues(_.map(_.toString))
    scala.xml.Unparsed(
      headerNames.map(metricMap.getOrElse("")
        .mkString("<td>","</td><td>","</td"))
  }

}

object GraphiteMetricSection {
  def metricTable(metrics: List[(String, Option[Double])]): NodeSeq={
    if (metrics.isEmpty)
      return NodeSeq.Empty
    <h1>Metrics</h1>
    <table>
      <tbody><tr><th>Metric Name</th><th>Metric Value</th></tr>
        {scala.xml.Unparsed( metrics.sortBy(_._1).map(kv=>s"<tr><td>${kv._1}</td><td>${kv._2.getOrElse("N/A")}</td></tr>").mkString("\r\n")  ) }
      </tbody>
    </table>
  }

  def metricTable(multimetrics  : Map[String, List[(String, Option[Double])]]) : NodeSeq = {
    if (multimetrics.values.flatten.map(_._2).filter(_.isDefined).isEmpty)
      return NodeSeq.Empty
    val bigTable = new GraphiteMetricTable(multimetrics.values.flatten.map(_._1).toList)
    <h1>Metrics</h1>
    <p>
      <table>
        <tbody><tr><th>Cluster</th>{bigTable.headerCells()}</tr>
          {multimetrics.toList.sortBy(_._1).map(kv=>{
            val clustername = kv._1
            val clusterMetrics = kv._2
            <tr><td>{clustername}</td>{bigTable.contentCells(clusterMetrics)}</tr>
          }).toSeq}
        </tbody>
      </table>
    </p>
  }


}
