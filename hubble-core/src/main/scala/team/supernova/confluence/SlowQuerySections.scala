package team.supernova.confluence

import team.supernova.cassandra.{SlowQuery, ClusterSlowQueries}

import scala.xml.NodeSeq

object SlowQuerySections {


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

  def slowQueryTable (queries: List[SlowQuery]) : NodeSeq = {
    Confluence.confluenceExpandBlock(s"Top ${queries.size} Slow queries",
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
      </table>)
  }

}
