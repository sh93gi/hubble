package team.supernova.confluence

import team.supernova.{ClusterInfo, Keyspace, Table}

import scala.xml.NodeSeq

object KeyspaceMetricsPage {

  def generateKeyspaceMetricsPage(keyspace: Keyspace, clusterInfo: ClusterInfo): String= {
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>{Confluence.CONFLUENCE_HEADER("This section summarises the keyspace metrics.")}<hr/>
      <h1>Keyspace: {keyspace.keyspace_name}</h1>
      <p>
        { SlowQuerySections.presentKeyspaceSlows(clusterInfo, keyspace.keyspace_name, expandBlock = false)  }
        { GraphiteMetricSection.singleMetricTable(
            clusterInfo.keyspaceMetrics.getOrElse(keyspace.keyspace_name, List()),
            <h1>Keyspace Metrics</h1>)}
      </p>
      <h1>Tables</h1>
      <p>
        <table>
          <tbody><tr><th>Table Name</th><th>Slow Queries</th></tr>
            { keyspace.tables.map(generateTablePart(_, keyspace, clusterInfo)) }
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

  def generateTablePart (table: Table, keyspace: Keyspace, clusterInfo: ClusterInfo): NodeSeq = {
    val full_name = s"${keyspace.keyspace_name}.${table.table_name}"
    if (SlowQuerySections.willPresentTableSlows(clusterInfo, full_name))
      <tr>
        <td>
          {table.table_name}
        </td>
        <td>
          {SlowQuerySections.presentTableSlows(clusterInfo, full_name)}
        </td>
      </tr>
    else
      NodeSeq.Empty
  }
}
