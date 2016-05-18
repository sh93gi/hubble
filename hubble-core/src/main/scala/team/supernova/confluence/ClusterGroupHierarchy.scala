package team.supernova.confluence

import java.util.Calendar

import team.supernova._
import team.supernova.cassandra.{ClusterSlowQueries, SlowQuery}
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage
import team.supernova.graphite.StringTemplate

import scala.collection.SortedSet
import scala.xml.NodeSeq

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
object ClusterGroupHierarchy {









  //TODO create Cluster page if not exits and group page if does not exist!
  //TODO CHeck if gets updated the first ever time
    def generateClusterGroupHierarchyPages(allClusters    : GroupClusters,
                                           project        : String,
                                           mainPageName   : String,
                                           token          : Token,
                                           deletePages    : Boolean): Unit = {

    //login into confluence
    val page: Page = new Page

    //Find the main Clusters page
    val parentPage: RemotePage = page.read(project, mainPageName)
    val tokenPageName = mainPageName + "-TOKEN"

    //read token page
    var tokenPage: RemotePage = null
    try {
      println (s"Reading $tokenPageName page")
      tokenPage = page.read(project, tokenPageName)
      println (s"$tokenPageName page found")
    }
    catch {
      case e: Exception =>
        val newPage: RemotePage = new RemotePage
        newPage.setTitle(tokenPageName)
        newPage.setParentId(parentPage.getId)
        newPage.setSpace(parentPage.getSpace)
        tokenPage = page.store(newPage)
        println (s"$tokenPageName page created!")
    }

    //Always update the Cluster page
    parentPage.setContent( s"<body>${ClusterGroupPage.generateClusterGroupPage(allClusters, project)}</body>")
    page.store(parentPage)

    var tokenContent: String = ""

    //Per ClusterInfo - create page
    for(clusterInfo <- allClusters.clusterInfoList)
      yield {
        val clusterPageName = clusterInfo.cluster_name.toUpperCase
        //create the specific summary cluster page
        tokenContent = tokenContent +"<br/>"+ Confluence.confluenceCreatePage (project,clusterPageName, ClusterSummaryPage.generateClusterSummaryPage(project, clusterInfo), page, parentPage, tokenPage )
        val clusterParentPage: RemotePage = page.read(project,clusterPageName)

        //Per keyspace create pages
        for(keyspace <- clusterInfo.keyspaces)
          yield {
            val content: String = KeyspacePage.generateKeyspacePage(keyspace, clusterInfo)
            //SEE DELETE below if you change this!!!!!!
            val keyPageName =  clusterInfo.cluster_name.toUpperCase + " - " + keyspace.keyspace_name.toUpperCase
            tokenContent = tokenContent +"<br/>"+ Confluence.confluenceCreatePage (project,keyPageName, content, page, clusterParentPage, tokenPage )
          }
      }


    //update the TOKEN page
    tokenPage.setContent(tokenContent)
    page.update(tokenPage, false)
    println (s"TOKEN page updated!")


   //clean up pages no longer needed - ie keyspace deleted
    //TODO add this method to confluence package??
     val clusterPages =  token.getService.getChildren(token.getToken, parentPage.getId)
    //clusterPages.foreach(p => println (p.getTitle))
    def substringAfter(s:String,k:String) = { s.indexOf(k) match { case -1 => ""; case i => s.substring(i+k.length)  } }
    //start bottom up
    for(cPage <- clusterPages)
      yield {
        val keyspacePages =  token.getService.getChildren(token.getToken, cPage.getId)
        for(kPage <- keyspacePages)
          yield {
            //SEE CREATE if you change this!!!!!!
            //Delete keyspace page if not exists
            if (allClusters.clusterInfoList.filter(cList => cList.cluster_name.toUpperCase.equals(cPage.getTitle)).
              flatMap(cl => cl.keyspaces).count(k => k.keyspace_name.toUpperCase.equals(substringAfter(kPage.getTitle," - "))) == 0 )
              {
                println (s"Found page to be deleted: ${kPage.getTitle}, DeletePage is $deletePages")
                if (deletePages) {
                  println (s"DELETING page: ${kPage.getTitle}")
                  page.remove(kPage.getId)
                  println (s"DELETED page: ${kPage.getTitle}")
                }
              }
          }

//TODO - REMOVED for now - need to rethink if this is handy
//        //Delete cluster page if not exists
//        if (!listClusterName.contains(cPage.getTitle))
//        {
//          println (s"DELETING page: ${cPage.getTitle}")
//          page.remove(cPage.getId)
//        }
      }
  }


}
