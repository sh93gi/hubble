package team.supernova.confluence

import java.util.Calendar

import org.slf4j.LoggerFactory
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage
import team.supernova.results.{ClusterInfo, Keyspace}
import team.supernova.validation.{Check, Severity}

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
              </tr>{clusterMap.toList.sortBy(_._1).flatMap {
              case (groupName, clusterInfoSet) =>
                val failedChecks = clusterInfoSet.flatMap(_.checks).filterNot(_.hasPassed)
                val errors = failedChecks.filter(_.severity==Severity.ERROR)
                val warnings = failedChecks.filter(_.severity==Severity.WARNING)
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
                      {
                        Confluence.confluenceCodeBlock("Error", errors.map(_.details).toList.sorted.mkString("\n"), "none")
                      }{
                        Confluence.confluenceCodeBlock("Warnings", warnings.map(_.details).toList.sorted.mkString("\n"), "none")
                      }
                    </td>
                    <td>
                      {Calendar.getInstance.getTime}
                    </td>
                  </tr>
            }}
            </tbody>
          </table>
        </p>
        <h2>Cluster Summary (Grouped by Cluster Group)</h2>
        <p>
          <table>
            <tbody>
              <tr>
                <th>Cluster Group Name</th> <th>Cluster Name</th><th>Total Keyspaces</th><th>Total Tables</th> <th>Total Errors</th> <th>Total Warnings</th> <th>Extras</th> <th>Last Checked</th>
              </tr>{clusterMap.toList.sortBy(_._1).flatMap {
              case (groupName, clusterInfoSet) =>
                val sortedClusterInfoSet = SortedSet[ClusterInfo]() ++ clusterInfoSet
                sortedClusterInfoSet.flatMap { clusterInfo =>
                  val checks: List[Check] = clusterInfo.checks
                  val errors = checks.filter(check => check.severity == Severity.ERROR && !check.hasPassed)
                  val warnings = checks.filter(check => check.severity == Severity.WARNING && !check.hasPassed)

                  val keyspaces: SortedSet[Keyspace] = clusterInfo.keyspaces
                  val numberOfTables = clusterInfo.keyspaces.map(_.children.size).sum
                  val numberOfKeyspaces = keyspaces.size

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
                        {
                        Confluence.confluenceCodeBlock("Error", errors.map(_.details).sorted.mkString("\n"), "none")
                        }{
                        Confluence.confluenceCodeBlock("Warnings", warnings.map(_.details).sorted.mkString("\n"), "none")
                        }
                      </td>
                      <td>
                        {Calendar.getInstance.getTime}
                      </td>
                    </tr>
                }
            }}
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
        logger.warn(s"Failed to find $pageName page, creating a new one.")
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