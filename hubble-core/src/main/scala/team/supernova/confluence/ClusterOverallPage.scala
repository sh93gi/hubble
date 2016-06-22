package team.supernova.confluence

import java.util.Calendar

import org.slf4j.LoggerFactory
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage
import team.supernova.validation.{Check, Severity}
import team.supernova.{ClusterInfo, Keyspace}

import scala.collection.SortedSet
import scala.util.{Failure, Success, Try}


object ClusterOverallPage {

  private val logger = LoggerFactory.getLogger(ClusterOverallPage.getClass)


  def generateOverallClustersPage(clusterMap: Map[String, Set[ClusterInfo]], token: Token, projectPage: String): Unit = {

    //login into confluence
    val page: Page = new Page

    //Find the main Clusters page
    val content =
      <body>
        {Confluence.CONFLUENCE_HEADER("This section briefly summarises all the cluster group information.")}<hr/>
        <h1>Cluster Group Summary</h1>
        <p>
          <table>
            <tbody>
              <tr>
                <th>Cluster Group Name</th> <th>Total Errors</th> <th>Total Warnings</th> <th>Extras</th> <th>Last Checked</th>
              </tr>{scala.xml.Unparsed(clusterMap.foldLeft("") {
              case (acc, (groupName, clusterInfoSet)) =>
                val checksForEachCluster = clusterInfoSet.map { clusterInfo =>
                  val errors = clusterInfo.checks.filter(check => check.severity == Severity.ERROR && !check.hasPassed)
                  val warnings = clusterInfo.checks.filter(check => check.severity == Severity.WARNING && !check.hasPassed)
                  (errors, warnings)
                }
                val totalChecks = checksForEachCluster.reduce((acc, next) => (acc._1 ++ next._1, acc._2 ++ next._2))
                val str = totalChecks match {
                  case (errors: List[Check], warnings: List[Check]) =>
                    <tr>
                      <td>
                        <a href={s"/display/$projectPage/${groupName.replace(" ", "+")}"}>
                          {groupName}
                        </a>
                      </td>
                      <td>
                        {errors.size}
                      </td>
                      <td>
                        {warnings.size}
                      </td>
                      <td>
                        {Confluence.confluenceCodeBlock("Error", errors.map(_.details).sorted.mkString("\n"), "none")}{Confluence.confluenceCodeBlock("Warnings", warnings.map(_.details).sorted.mkString("\n"), "none")}
                      </td>
                      <td>
                        {Calendar.getInstance.getTime}
                      </td>
                    </tr>.toString()
                }
                acc + str
            })}
            </tbody>
          </table>
        </p>
        <h2>Cluster Summary (Grouped by Cluster Group)</h2>
        <p>
          <table>
            <tbody>
              <tr>
                <th>Cluster Group Name</th> <th>Cluster Name</th><th>Total Keyspaces</th><th>Total Tables</th> <th>Total Errors</th> <th>Total Warnings</th> <th>Extras</th> <th>Last Checked</th>
              </tr>{scala.xml.Unparsed(clusterMap.foldLeft("") {
              case (acc, (groupName, clusterInfoSet)) =>
                val sortedClusterInfoSet = SortedSet[ClusterInfo]() ++ clusterInfoSet
                var clusterDetails = ""
                sortedClusterInfoSet.foreach { clusterInfo =>
                  val checks: List[Check] = clusterInfo.checks
                  val errors = checks.filter(check => check.severity == Severity.ERROR && !check.hasPassed)
                  val warnings = checks.filter(check => check.severity == Severity.WARNING && !check.hasPassed)

                  val keyspaces: SortedSet[Keyspace] = clusterInfo.keyspaces
                  val numberOfTables = keyspaces.foldLeft(0)((acc, right) => acc + right.children.size)
                  val numberOfKeyspaces = keyspaces.size

                  clusterDetails +=
                    <tr>
                      <td>
                        <a href={s"/display/$projectPage/${groupName.replace(" ", "+")}"}>
                          {groupName}
                        </a>
                      </td>
                      <td>
                        <a href={s"/display/$projectPage/${clusterInfo.cluster_name.replace(" ", "+")}"}>
                          {clusterInfo.cluster_name}
                        </a>
                      </td>
                      <td>
                        {numberOfKeyspaces}
                      </td>
                      <td>
                        {numberOfTables}
                      </td>
                      <td>
                        {errors.size}
                      </td>
                      <td>
                        {warnings.size}
                      </td>
                      <td>
                        {Confluence.confluenceCodeBlock("Error", errors.map(_.details).sorted.mkString("\n"), "none")}{Confluence.confluenceCodeBlock("Warnings", warnings.map(_.details).sorted.mkString("\n"), "none")}
                      </td>
                      <td>
                        {Calendar.getInstance.getTime}
                      </td>
                    </tr>.toString()
                }
                acc + clusterDetails
            })}
            </tbody>
          </table>
        </p>
      </body>.toString()

    val pageName: String = "Clusters"
    val remotePage = Try {
      page.read(projectPage, pageName)
    } match {
      case Success(pageFound) => pageFound
      case Failure(f) =>
        val newPage = new RemotePage()
        newPage.setTitle(pageName)
        newPage.setSpace(projectPage)
        newPage.setContent("")
        page.store(newPage)
        newPage
    }
    remotePage.setContent(content)
    page.update(remotePage, false)
  }
}