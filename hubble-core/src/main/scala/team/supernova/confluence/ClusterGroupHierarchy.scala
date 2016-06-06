package team.supernova.confluence

import org.slf4j.LoggerFactory
import team.supernova._
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.{RemotePage, RemotePageSummary}

object ConfluenceNaming {
  def createName(cluster: ClusterInfo, keyspace: Keyspace): String = {
    cluster.cluster_name.toUpperCase + " - " + keyspace.keyspace_name.toUpperCase
  }

  def hasName(cluster: ClusterInfo, page: RemotePageSummary): Boolean = {
    cluster.cluster_name.toUpperCase.equals(page.getTitle)
  }

  def substringAfter(s: String, k: String) = {
    s.indexOf(k) match {
      case -1 => "";
      case i => s.substring(i + k.length)
    }
  }

  def hasName(keyspace: Keyspace, page: RemotePageSummary): Boolean = {
    keyspace.keyspace_name.toUpperCase.equals(substringAfter(page.getTitle, " - "))
  }

}

object ClusterGroupHierarchy {
  val log = LoggerFactory.getLogger(ClusterGroupHierarchy.getClass)

  //TODO create Cluster page if not exits and group page if does not exist!
  //TODO CHeck if gets updated the first ever time
  def generateClusterGroupHierarchyPages(allClusters: GroupClusters,
                                         project: String,
                                         mainPageName: String,
                                         token: Token,
                                         deletePages: Boolean): Unit = {

    //login into confluence
    val page: Page = new Page

    //Find the main Clusters page
    val clusterGroupPage: RemotePage = page.read(project, mainPageName)
    val tokenPageName = mainPageName + "-TOKEN"

    //read token page
    var tokenPage: RemotePage = null
    try {
      log.info(s"Reading tokenpage '$tokenPageName'")
      tokenPage = page.read(project, tokenPageName)
      log.info(s"Found tokenpage '$tokenPageName'")
    }
    catch {
      case e: Exception =>
        val newPage: RemotePage = new RemotePage
        newPage.setTitle(tokenPageName)
        newPage.setParentId(clusterGroupPage.getId)
        newPage.setSpace(clusterGroupPage.getSpace)
        tokenPage = page.store(newPage)
        log.info(s"Created tokenpage '$tokenPageName'")
    }

    //Always update the Cluster page
    clusterGroupPage.setContent(s"<body>${ClusterGroupPage.generateClusterGroupPage(allClusters, project)}</body>")
    page.store(clusterGroupPage)

    var tokenContent: String = ""

    //Per ClusterInfo - create page
    for (clusterInfo <- allClusters.clusterInfoList)
      yield {
        val clusterPageName = clusterInfo.cluster_name.toUpperCase
        //create the specific summary cluster page
        val clusterContent = ClusterSummaryPage.generateClusterSummaryPage(project, clusterInfo)
        val clusterPageHash = Confluence.confluenceCreatePage(project, clusterPageName, clusterContent, page, clusterGroupPage, tokenPage)
        tokenContent = tokenContent + "<br/>" + clusterPageHash
        val clusterParentPage: RemotePage = page.read(project, clusterPageName)

        //Per keyspace create pages
        for (keyspace <- clusterInfo.keyspaces)
          yield {
            val keyspaceContent = KeyspacePage.generateKeyspacePage(keyspace, clusterInfo)
            val keyPageName = ConfluenceNaming.createName(clusterInfo, keyspace)
            val keyspacePageHash = Confluence.confluenceCreatePage(project, keyPageName, keyspaceContent, page, clusterParentPage, tokenPage)
            tokenContent = tokenContent + "<br/>" + keyspacePageHash
          }
      }

    //update the TOKEN page
    tokenPage.setContent(tokenContent)
    page.update(tokenPage, false)
    log.info(s"TOKEN page updated!")

    //clean up pages no longer needed - ie keyspace deleted
    val clusterPages = token.getService.getChildren(token.getToken, clusterGroupPage.getId)
    for (cPage <- clusterPages)
      yield {
        val keyspacePages = token.getService.getChildren(token.getToken, cPage.getId)
        for (kPage <- keyspacePages)
          yield {
            //Delete keyspace page if not exists
            if (allClusters.clusterInfoList.filter(clusterInfo => ConfluenceNaming.hasName(clusterInfo, cPage)).
              flatMap(cl => cl.keyspaces).count(k => ConfluenceNaming.hasName(k, kPage)) == 0) {
              log.info(s"Found page to be deleted: ${kPage.getTitle}, DeletePage is $deletePages")
              if (deletePages) {
                log.info(s"DELETING page: ${kPage.getTitle}")
                page.remove(kPage.getId)
                log.info(s"DELETED page: ${kPage.getTitle}")
              }
            }
          }

        //TODO - REMOVED for now - need to rethink if this is handy
        //        //Delete cluster page if not exists
        //        if (!listClusterName.contains(cPage.getTitle))
        //        {
        //          log.info(s"DELETING page: ${cPage.getTitle}")
        //          page.remove(cPage.getId)
        //        }
      }
  }


}
