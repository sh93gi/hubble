package team.supernova.confluence

import org.slf4j.LoggerFactory
import team.supernova.ClusterInfo

import scala.xml.NodeSeq

object ClusterMetricsPage {
  val log = LoggerFactory.getLogger(ClusterMetricsPage.getClass)

  def generateClusterMetricsPage(project: String, clusterInfo: ClusterInfo): String = {
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>
      {Confluence.CONFLUENCE_HEADER("This section summarises the cluster metrics.")}<hr/>
      <h1>Cluster:
        {clusterInfo.cluster_name}
      </h1>
      {
        GraphitePlotSection.present(clusterInfo.cluster.graphiteConfig.graphite_plot, clusterInfo.cluster.graphite)}
        {SlowQuerySections.presentClusterSlows(clusterInfo, expandBlock = false)}
        {GraphiteMetricSection.singleMetricTable(clusterInfo.clusterMetrics, <h1>Cluster Metrics</h1>)}
        { GraphiteMetricSection.combinedMetricTable(
            clusterInfo.keyspaceMetrics,
            <h1>Keyspace Metrics Summary</h1>,
            "Keyspace",
            Some(ConfluenceNaming.createMetricsLink(project, clusterInfo.cluster_name, _))
          ) }
      <h1>Keyspace Slow Queries</h1>
      <p>
        <table>
          <tbody>
            <tr>
              <th>Keyspace Name</th> <th>Slow Queries</th>
            </tr>{keyspaceInfoRows(project, clusterInfo)}
          </tbody>
        </table>
      </p>
      <h1>Tables</h1>
      <p>
        <table>
          <tbody>
            {tableInfoRows(project, clusterInfo)}
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

  def keyspaceInfoRows(project: String, clusterInfo: ClusterInfo): NodeSeq = {
    clusterInfo.keyspaces
      .filter(k=>SlowQuerySections.willPresentKeySpaceSlows(clusterInfo, k.keyspace_name))
      .toSeq.flatMap(k => {
        val href = ConfluenceNaming.createLink(project, clusterInfo, k)
        <tr>
          <td>
            <a href={href}>
              {k.keyspace_name}
            </a>
          </td>
          <td>
            {SlowQuerySections.presentKeyspaceSlows(clusterInfo, k.keyspace_name)}
          </td>
        </tr>
      })
  }

  def tableInfoRows(project: String, clusterInfo: ClusterInfo): NodeSeq = {
    try {
      val rows = clusterInfo.opsCenterClusterInfo.map(opsinfo => opsinfo.nodes.
        flatMap(n => n.opsKeyspaceInfoList.flatMap(k => k.opsTableInfoList.map(t => Tuple5(k.keyspaceName, t.tableName, n.name, t.avgDataSizeMB, t.numberSSTables))))
        .groupBy(_._1).toSeq.sortBy(_._1)
        .flatMap(keyspace => {
          val href = ConfluenceNaming.createLink(project, clusterInfo.cluster_name, keyspace._1)
          keyspace._2.groupBy(_._2).toSeq.sortBy(_._1).map(table =>
            //first row
            <tr>
              <td>
                <a href={href}>
                  {keyspace._1}
                </a>
              </td>
              <td>
                {table._1}
              </td>
              <td>
                {scala.xml.Unparsed(table._2.sortBy(_._3).foldLeft("") { (c, n) => c + s"<p>${n._3} - ${
                if (n._5.toInt.equals(-1)) {
                  " ERROR retrieving stats"
                } else {
                  s"${n._4}MB - ${n._5} sstables"
                }
              } </p>"
              })}
              </td>
              <td>
                {table._2.foldLeft(0.toLong) { (c, n) => c + n._4 }}
                MB</td>
              <td>
                {table._2.map(_._4).min}
                MB</td>
              <td>
                {table._2.map(_._4).max}
                MB</td>
              <td>
                {table._2.map(_._4).max - table._2.map(_._4).min}
                MB</td>
              <td>
                {table._2.foldLeft(0.toLong) { (c, n) => c + n._5 }}
              </td>
            </tr>
          )
        })).getOrElse(NodeSeq.Empty)

      <tr>
        <th>Keyspace Name</th> <th>Table</th> <th>Node</th> <th>Total Size</th> <th>Min Size</th> <th>Max Size</th> <th>Difference (Max-Min)</th> <th>Total SSTables</th>
      </tr> ++
        rows
    }
    catch {
      case e: Exception =>
        log.error("Failed to create table details.", e)
        NodeSeq.Empty
    }
  }

}
