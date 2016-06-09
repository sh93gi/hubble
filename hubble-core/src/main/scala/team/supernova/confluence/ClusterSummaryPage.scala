package team.supernova.confluence

import team.supernova.ClusterInfo
import team.supernova.validation.Severity

import scala.collection.SortedSet

object ClusterSummaryPage {

  def generateClusterSummaryPage(project: String, clusterInfo: ClusterInfo): String= {

    val clusterWarnings = clusterInfo.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.WARNING)).map(_.details).sorted.mkString("\n")
    val clusterErrors = clusterInfo.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.ERROR)).map(_.details).sorted.mkString("\n")

    //The actual cluster page itself
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>{Confluence.CONFLUENCE_HEADER("This section summarises all the cluster information.")}<hr/>
      <h1>Cluster: {clusterInfo.cluster_name}</h1>
      <p><a href={ConfluenceNaming.createMetricsLink(project, clusterInfo)}>Detailed cluster metrics</a>
      { Confluence.confluenceCodeBlock("Errors", clusterErrors ,"none")}
        { Confluence.confluenceCodeBlock("Warnings", clusterWarnings ,"none")}
      </p>
      <h1>Host Information</h1>
      <p>
        <table>
          <tbody><tr><th>Data Center</th><th>Host Name</th><th>IP Address</th><th>Rack</th><th>C* Version</th><th>Extras</th></tr>
            {clusterInfo.hosts.to[SortedSet].map(host =>
              <tr>
                <td>{host.dataCenter}</td>
                <td>{host.canonicalHostName}</td>
                <td>{host.ipAddress}</td>
                <td>{host.rack}</td>
                <td>{host.version}</td>
                <td>{CassandraYamlSection.presentYamlShort(host.opsCenterNode.flatMap(_.cassandra))}</td>
              </tr>
            ).toSeq
            }
          </tbody>
        </table>
      </p>
      <h1>Node Yaml comparison</h1>
      {
        val hostVsYaml = clusterInfo.hosts.toList.sortBy(_.canonicalHostName)
          .map(host=>(
            host.canonicalHostName,
            host.opsCenterNode.flatMap(_.cassandra)
            ))
        CassandraYamlSection.presentYamlCompare(hostVsYaml)
      }
      <h1>Keyspaces</h1>
      <p>
        <table>
          <tbody><tr><th>Keyspace Name</th><th>Extras</th></tr>
            {scala.xml.Unparsed( keyspaceInfoRows(project, clusterInfo) )}
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

  def keyspaceInfoRows(project: String, clusterInfo: ClusterInfo): String = {
    val rows = clusterInfo.keyspaces.foldLeft(""){(a,k) =>
      val warnings = k.checks.filter(!_.hasPassed).map(_.details).sorted.mkString("\n")

      val href = s"/display/$project/${ConfluenceNaming.createName(clusterInfo, k).replace(" ","+")}"
      a +
        <tr>
          <td><a href={href}>{k.keyspace_name}</a></td>
          <td>
            { Confluence.confluenceCodeBlock("Schema",k.schemaScript,"none")}
            { Confluence.confluenceCodeBlock("Warnings",warnings,"scala")}
          </td>
        </tr>
    }
    rows
  }
}
