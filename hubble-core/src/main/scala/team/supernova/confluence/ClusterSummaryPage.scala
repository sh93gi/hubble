package team.supernova.confluence

import team.supernova.{ClusterInfo, _}

import scala.collection.SortedSet

object ClusterSummaryPage {

  def generateClusterSummaryPage(project: String, clusterInfo: ClusterInfo): String= {

    def keyspaceInfoRows (clusterInfo: ClusterInfo): String = {
      val rows = clusterInfo.keyspaces.foldLeft(""){(a,k) =>
        val warnings = k.checks.filter(!_.hasPassed).map(_.details).sorted.mkString("\n")

        val href = s"/display/$project/${clusterInfo.cluster_name.replace(" ","+")}+-+${k.keyspace_name}"
        a +
          <tr>
            <td><a href={href}>{k.keyspace_name}</a></td>
            <td>
              { Confluence.confluenceCodeBlock("Schema",k.schemaScript,"none")}
              { Confluence.confluenceCodeBlock("Warnings",warnings,"scala")}
              { SlowQuerySections.presentKeyspace(clusterInfo, k.keyspace_name) }
            </td>
          </tr>
      }
      rows
    }


    def tableInfoRows (clusterInfo: ClusterInfo): String = {
      try {
        //        val tuples = clusterInfo.opsCenterClusterInfo.get.nodes.
        //          flatMap(n => n.opsKeyspaceInfoList.flatMap(k=> k.opsTableInfoList.map(t=> Tuple5(k.keyspaceName, t.tableName, n.name, t.avgDataSizeMB, t.numberSSTables )))).sorted
        //        val x = tuples.groupBy(_._1).flatMap(a => a._2).groupBy(_._2)


        val rows = clusterInfo.opsCenterClusterInfo.map( opsinfo=> opsinfo.nodes.
          flatMap(n => n.opsKeyspaceInfoList.flatMap(k=> k.opsTableInfoList.map(t=> Tuple5(k.keyspaceName, t.tableName, n.name, t.avgDataSizeMB, t.numberSSTables ))))
          .groupBy(_._1).toSeq.sortBy(_._1)
          .foldLeft(""){(a,keyspace) =>
            val href = s"/display/$project/${clusterInfo.cluster_name.replace(" ", "+")}+-+${keyspace._1}"

            a +
              keyspace._2.groupBy(_._2).toSeq.sortBy(_._1).foldLeft(""){(b, table) =>
                //first row
                b +
                  <tr>
                    <td><a href={href}>{keyspace._1}</a></td>
                    <td>{table._1}</td>
                    <td>{scala.xml.Unparsed( table._2.toSeq.sortBy(_._3).foldLeft(""){(c, n)=> c + s"<p>${n._3} - ${ if (n._5.toInt.equals(-1)) {" ERROR retrieving stats"} else{s"${n._4}MB - ${n._5} sstables"}} </p>"})} </td>
                    <td>{table._2.foldLeft(0.toLong){(c, n)=> c + n._4}}MB</td>
                    <td>{table._2.map(_._4).min}MB</td>
                    <td>{table._2.map(_._4).max}MB</td>
                    <td>{table._2.map(_._4).max - table._2.map(_._4).min}MB</td>
                    <td>{table._2.foldLeft(0.toLong){(c, n)=> c + n._5}}</td>
                  </tr>
              }
          }).getOrElse("")

        <tr><th>Keyspace Name</th><th>Table</th><th>Node</th><th>Total Size</th><th>Min Size</th><th>Max Size</th><th>Difference (Max-Min)</th><th>Total SSTables</th></tr> +
          rows
      }
      catch {case e: Exception =>
        println(s"$e")
        ""
      }
    }

    val clusterWarnings = clusterInfo.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.WARNING)).map(_.details).sorted.mkString("\n")
    val clusterErrors = clusterInfo.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.ERROR)).map(_.details).sorted.mkString("\n")


    //The actual cluster page itself
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>{Confluence.CONFLUENCE_HEADER("This section summarises all the cluster information.")}<hr/>
      <h1>Cluster: {clusterInfo.cluster_name}</h1>
      <p>{ Confluence.confluenceCodeBlock("Errors", clusterErrors ,"none")}
        { Confluence.confluenceCodeBlock("Warnings", clusterWarnings ,"none")}
      </p>
      { GraphitePlotSection.present(clusterInfo.cluster.graphiteConfig.graphite_plot, clusterInfo.cluster.graphite) }
      { SlowQuerySections.presentCluster(clusterInfo) }
      { GraphiteMetricSection.singleMetricTable(clusterInfo.metrics)}
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
                <td>{CassandraYamlSection.presentYamlShort(host.opsCenterNode.map(_.cassandra))}</td>
              </tr>
            ).toSeq
            }
          </tbody>
        </table>
      </p>
      {
        val hostVsYaml = clusterInfo.hosts.toList.sortBy(_.canonicalHostName)
          .map(host=>(
            host.canonicalHostName,
            host.opsCenterNode.map(_.cassandra)
            ))
        CassandraYamlSection.presentYamlCompare(hostVsYaml)
      }
      <h1>Keyspaces</h1>
      <p>
        <table>
          <tbody><tr><th>Keyspace Name</th><th>Extras</th></tr>
            {scala.xml.Unparsed( keyspaceInfoRows (clusterInfo) )}
          </tbody>
        </table>
      </p>
      <h1>Tables</h1>
      <p>
        <table>
          <tbody>
            {scala.xml.Unparsed( tableInfoRows (clusterInfo) )}
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

}
