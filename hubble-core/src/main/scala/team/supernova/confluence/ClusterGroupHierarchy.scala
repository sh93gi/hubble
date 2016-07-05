package team.supernova.confluence

import org.slf4j.LoggerFactory
import team.supernova._
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.{RemotePage, RemotePageSummary}

import scala.collection.mutable.ArrayBuffer

class ClusterGroupHierarchy(project: String, page: Page, tokenPage: RemotePage){
  val log = LoggerFactory.getLogger(classOf[ClusterGroupHierarchy])
  var tokenContent: String = ""
  var expectedTitles = ArrayBuffer[String](tokenPage.getTitle)

  def replaceWithToken(current: RemotePage, title: String, content: String): RemotePage ={
    Confluence.confluenceReplacePage(current, title, content, page, tokenPage) match {
      case (clusterPageHash, resultPage) =>
        tokenContent = tokenContent + "<br/>" + clusterPageHash
        expectedTitles += title
        resultPage
    }
  }

  def createWithToken(title: String, content: String, parent: RemotePage): RemotePage ={
    Confluence.confluenceCreatePage(project, title, content, page, parent, tokenPage) match {
      case (clusterPageHash, resultPage) =>
        tokenContent = tokenContent + "<br/>" + clusterPageHash
        expectedTitles += title
        resultPage
    }
  }

  def createTokenless(title: String, content: String, parent: RemotePage):Unit = {
    Confluence.confluenceCreateTokenLessPage(project, title, content, page, parent, notify = false)
    expectedTitles += title
  }

  def updateToken(){
    val newTokenPage = page.read(tokenPage.getId)
    newTokenPage.setContent(tokenContent)
    page.update(newTokenPage, false)
    log.info(s"TOKEN page updated!")
  }

  def contains(title: String): Boolean ={
    expectedTitles.contains(title)
  }

  def getDepth(child: RemotePageSummary, parent: RemotePageSummary): Option[Int] = {
    var currentChild = child
    if (child.getId == parent.getId)
      return Some(0)
    var depth=1
    while(currentChild.getParentId!=parent.getId) {
      currentChild = page.read(currentChild.getParentId)
      depth+=1
      if (depth == 10)
        return None
    }
    Some(depth)
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
    val tokenPage: RemotePage = Confluence.confluenceReadOrCreate(project, tokenPageName, page, clusterGroupPage)

    val hierarchy = new ClusterGroupHierarchy(project, page, tokenPage)
    //Always update the Cluster page
    clusterGroupPage.setContent(s"<body>${ClusterGroupPage.generateClusterGroupPage(allClusters, project)}</body>")
    page.store(clusterGroupPage)


    //Per ClusterInfo - create page
    for (clusterInfo <- allClusters.clusterInfoList)
      yield {
        //create the specific summary cluster page
        val clusterPageName = ConfluenceNaming.createName(clusterInfo)
        val clusterContent = ClusterSummaryPage.generateClusterSummaryPage(project, clusterInfo)
        val clusterParentPage = hierarchy.createWithToken(clusterPageName, clusterContent, clusterGroupPage)


        // Create different page for metrics of cluster, without notifications.
        val clusterMetricsContent = ClusterMetricsPage.generateClusterMetricsPage(project, clusterInfo)
        val clusterMetricsPageName = ConfluenceNaming.createMetricsName(clusterInfo)
        hierarchy.createTokenless(clusterMetricsPageName, clusterMetricsContent, clusterParentPage)

        // Per keyspace create pages
        for (keyspace <- clusterInfo.keyspaces)
          yield {
            val keyspaceContent = KeyspacePage.generateKeyspacePage(project, keyspace, clusterInfo)
            val keyspacePageName = ConfluenceNaming.createName(clusterInfo, keyspace)
            val keyspacePage = Confluence.getIfExists(project, ConfluenceNaming.createDeletedName(keyspacePageName), page) match{
              case None=> hierarchy.createWithToken(keyspacePageName, keyspaceContent, clusterParentPage)
              case Some(deleted) => hierarchy.replaceWithToken(deleted, keyspacePageName, keyspaceContent)
            }


            // Create different page for metrics of keyspace, without notification
            val keyspaceMetricsContent = KeyspaceMetricsPage.generateKeyspaceMetricsPage(keyspace, clusterInfo)
            val keyspaceMetricsPageName = ConfluenceNaming.createMetricsName(clusterInfo, keyspace)
            hierarchy.createTokenless(keyspaceMetricsPageName, keyspaceMetricsContent, keyspacePage)
          }
      }

    //update the TOKEN page
    hierarchy.updateToken()


    //clean up pages no longer needed - ie keyspace deleted
    val keyspace_depth = 2
    for (kPage <- token.getService.getDescendents(token.getToken, clusterGroupPage.getId)
          if !hierarchy.contains(kPage.getTitle)) {
      val depth = hierarchy.getDepth(kPage, clusterGroupPage)
      log.info(s"Found page at depth $depth to be deleted: ${kPage.getTitle}, DeletePage is $deletePages")
      depth match{
        case Some(x) if x >= keyspace_depth =>  //Does not delete clusters
          if (deletePages || kPage.getTitle.endsWith("metrics")){
            log.info(s"DELETING page: ${kPage.getTitle}")
            page.remove(kPage.getId)
            log.info(s"DELETED page: ${kPage.getTitle}")
          } else {
            if (!deletePages && !ConfluenceNaming.hasDeletedName(kPage.getTitle)){
              val kPageFull = page.read(kPage.getId)
              val parentPage = page.read(kPage.getParentId)
              // If the keyspace of the page is of a cluster we've actually verified this run
              if (allClusters.clusterInfoList.count(ConfluenceNaming.hasNameOf(_, parentPage)) > 0) {
                // Then rename it
                kPageFull.setTitle(ConfluenceNaming.createDeletedName(kPage.getTitle))
                log.info(s"RENAMING page: ${kPage.getTitle} to ${kPageFull.getTitle}")
                page.store(kPageFull)
              }
            }
          }
        case Some(x)=>
          log.info(s"Not deleting page: ${kPage.getTitle} because of wrong depth compared to space")
        case None=>
          log.info(s"Not deleting page: ${kPage.getTitle} because of unknown depth compared to space")
      }
    }
  }


}
