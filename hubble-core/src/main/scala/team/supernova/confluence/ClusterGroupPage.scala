package team.supernova.confluence

import java.util.Calendar

import team.supernova.GroupClusters
import team.supernova.validation.Severity

import scala.collection.SortedSet

object ClusterGroupPage {
  //TODO - make graphite checker!!!  Seems to be rather messy
  //this is using graphana!
  def clusterGraphite(cluster_name: String, graphana_host: String ) = <a href={s"http://$graphana_host/dashboard/db/cluster-health-per-cluster?Cluster=$cluster_name"}>Overview</a>

  def generateClusterGroupPage(groupClusters: GroupClusters, project: String): String=  {

    val listKeyspace: SortedSet[String] = groupClusters.clusterInfoList.flatMap(_.keyspaces).map(_.keyspace_name)

    //this must not be sorted as it is already sorted - just need the list of names!
    val listClusterName: List[String] = groupClusters.clusterInfoList.foldLeft(List[String]()){(a,b) => a ++ List(b.cluster_name)}
    println (s"Cluster List: $listClusterName")

    def whichColourClassBoolean(isTrue: Boolean): String =  if (isTrue) {"highlight-green confluenceTd"} else {"highlight-red confluenceTd"}

    <body>{Confluence.CONFLUENCE_HEADER("This section briefly summarises all the clusters information.")}<hr/>
      <h1>Cluster Summary</h1>
      <p>
        <table>
          <tbody><tr><th>Cluster Name</th><th>Metrics</th><th>Total Errors</th><th>Total Warnings</th><th>Extras</th><th>Last Checked</th></tr>
            {scala.xml.Unparsed( groupClusters.clusterInfoList.foldLeft("") { (at, clus) =>
            val warnings = clus.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.WARNING))
            val errors = clus.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.ERROR))
            at +
              <tr>
                <td><a href={s"/display/$project/${clus.cluster_name.replace(" ","+")}"}>{clus.cluster_name}</a></td>
                <td>{clusterGraphite(clus.cluster_name, clus.cluster.graphana)}</td>
                <td>{errors.size}</td>
                <td>{warnings.size}</td>
                <td>{ Confluence.confluenceCodeBlock("Error", errors.map(_.details).sorted.mkString("\n"), "none")}
                  { Confluence.confluenceCodeBlock("Warnings", warnings.map(_.details).sorted.mkString("\n"), "none")}
                </td>
                <td>{ Calendar.getInstance.getTime} </td>
              </tr>
          } )
            }
          </tbody>
        </table>
      </p>
      { GraphiteMetricSection.combinedMetricTable(groupClusters.clusterInfoList.map(clusterInfo=>(clusterInfo.cluster_name, clusterInfo.metrics)).toMap)}
      <h1>Cluster Yaml comparison</h1>
      {
        val keyVsYaml = groupClusters.clusterInfoList.toList
          .map(clusterInfo=>(clusterInfo.cluster_name, clusterInfo.opsCenterClusterInfo
          .map(_.nodes.headOption.map(_.cassandra)).flatten.getOrElse(None)))
        CassandraYamlSection.presentYamlCompare(keyVsYaml)
      }
      <h1>Cluster Keyspace Summary</h1>
      <p>
        <table>
          <tbody>
            <tr>
              <th>Keyspace Name</th>
              {scala.xml.Unparsed( listClusterName.foldLeft("") { (acc, clust_name) => acc +
              <th>{clust_name}</th> + <th>#Tables</th>})
              }
            </tr>
            {scala.xml.Unparsed( listKeyspace.foldLeft("") { (acc, key: String) => acc +
            <tr>
              <td>{ key }</td>
              {scala.xml.Unparsed( listClusterName.foldLeft("") { (acc, clust_name) =>
              val isFound = groupClusters.clusterInfoList.filter(a => a.cluster_name.equals(clust_name)).flatMap(_.keyspaces).count(k => k.keyspace_name.equals(key)) > 0
              //TODO fix LINKS!! make more reusable
              acc + <td class={whichColourClassBoolean(isFound)}>{ if (isFound) {<a href={s"/display/$project/${clust_name.replace(" ","+")}+-+$key"}>{isFound}</a>} else isFound }</td> +
                <td class={whichColourClassBoolean(isFound)}>{ if (isFound) {groupClusters.clusterInfoList.filter(a => a.cluster_name.equals(clust_name)).flatMap(_.keyspaces).filter(_.keyspace_name == key).head.tables.size.toString} else ""}</td>})
              }
            </tr>
          } )
            }
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

}
