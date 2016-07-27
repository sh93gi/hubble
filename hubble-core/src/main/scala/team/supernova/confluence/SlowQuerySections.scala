package team.supernova.confluence

import team.supernova.cassandra.{ClusterSlowQueries, SlowQuery}
import team.supernova.results.ClusterInfo

import scala.xml.NodeSeq

object SlowQuerySections {
  def willPresentKeySpaceSlows(clusterInfo: ClusterInfo, keyspaceName: String): Boolean ={
    clusterInfo.slowQueries.keyspaceSlow
      .get(keyspaceName)
        .map(_.get(1).size).sum > 0
  }

  def presentKeyspaceSlows(clusterInfo: ClusterInfo, keyspaceName: String, expandBlock: Boolean = true): NodeSeq ={
    clusterInfo.slowQueries.keyspaceSlow
        .get(keyspaceName)
        .map(topSlowest =>
          presenterFor(expandBlock)(topSlowest.get(10))).getOrElse(NodeSeq.Empty)
  }

  def presentClusterSlows(clusterInfo: ClusterInfo, expandBlock: Boolean = true): NodeSeq ={
    val slows = <p>{
      val slowToShow = clusterInfo.slowQueries.clusterSlow.get(10)
      if (slowToShow.isEmpty)
        NodeSeq.Empty
      else presenterFor(expandBlock)(clusterInfo.slowQueries.clusterSlow.get(10))}</p>
    val errors =
      if (clusterInfo.slowQueries.failed.nonEmpty)
        <p>{SlowQuerySections.slowQueryFailures(clusterInfo.slowQueries)}</p>
      else
        NodeSeq.Empty
    slows ++ errors
  }

  def slowQueryFailures(clusterSlowQueries: ClusterSlowQueries) : NodeSeq = {
    if (clusterSlowQueries.unauthorized.isEmpty &&
      clusterSlowQueries.failed.isEmpty)
      return NodeSeq.Empty
    Confluence.confluenceExpandBlock("Slow query retrieval failures",
      <p>
        {scala.xml.Unparsed(
        clusterSlowQueries.unauthorized.map(e=>e.getMessage).map(scala.xml.Text(_)).mkString("<br/>") +
          clusterSlowQueries.failed.map(e=>e.getMessage).map(scala.xml.Text(_)).mkString("<br/>"))}
      </p>
    )
  }

  def willPresentTableSlows(clusterInfo: ClusterInfo, tableFullName: String): Boolean = {
    clusterInfo.slowQueries.tableSlow.get(tableFullName).map(topSlowest =>
      topSlowest.get(1).size).sum > 0
  }

  def presentTableSlows(clusterInfo: ClusterInfo, tableFullName: String, expandBlock: Boolean = true): NodeSeq = {
    clusterInfo.slowQueries.tableSlow.get(tableFullName).map(topSlowest =>
      presenterFor(expandBlock)(topSlowest.get(10))).getOrElse(NodeSeq.Empty)
  }

  def presenterFor(expandBlock: Boolean) : (List[SlowQuery]=>NodeSeq) = {
    if (expandBlock)
      slowQueryTableExpandBlock
    else
      slowQueryTableParagraph
  }

  def slowQueryTableParagraph(queries: List[SlowQuery]) : NodeSeq = {
    <h1>Top {queries.size} slow queries</h1>
    <p>
      {slowQueryTable(queries: List[SlowQuery])}
    </p>
  }

  def slowQueryTableExpandBlock(queries: List[SlowQuery]) : NodeSeq = {
    Confluence.confluenceExpandBlock(s"Top ${queries.size} Slow queries",
      slowQueryTable(queries: List[SlowQuery]) )
  }

  def slowQueryTable(queries: List[SlowQuery]) : NodeSeq = {
    <table>
      <tbody><tr><th>Duration (ms)</th><th>Commands</th><th>Keyspaces</th><th>Tables</th></tr>
        {scala.xml.Unparsed( queries.foldLeft("") { (txt, slowQuery) => txt +
        <tr>
          <td>{slowQuery.duration}</td>
          <td>{slowQuery.commands.map(scala.xml.Text(_)).mkString("<br/>")}</td>
          <td>{slowQuery.keyspaces.mkString("<br/>")}</td>
          <td>{slowQuery.tables.mkString("<br/>")}</td>
        </tr>
      })
        }
      </tbody>
    </table>
  }

}
