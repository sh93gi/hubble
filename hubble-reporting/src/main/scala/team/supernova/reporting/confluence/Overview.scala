package team.supernova.reporting.confluence

import org.slf4j.LoggerFactory
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage

import scala.xml.Elem

object Overview {

case class KeyspaceCluster (keyspace: String, clusterName: String)

case class KeyspaceInfo (keyspace       : String,
                         clusterName    : String,
                         linkedCluster  : String,
                         dcabGroup      : String,
                         dcabApprover   : List[String],
                         devContacts    : List[String],
                         opsContacts    : List[String],
                         productOwner   : List[String],
                         app            : String,
                         desc          : String)
  val logger = LoggerFactory.getLogger(classOf[KeyspaceInfo])



  //return all the keyspaces name per cluster - TODO perhaps per group
  def getKeyspaces (token: Token) : List[KeyspaceCluster] = {
    var keyspaceInfoList: List[KeyspaceCluster] = List[KeyspaceCluster]()

    val pageName = "Clusters"
    val page: Page = new Page
    val parentPage: RemotePage = page.read("kaas", pageName)
    val groupPages =  token.getService.getChildren(token.getToken, parentPage.getId)

    //group pages e.g. LLDS_1
    for(gPage <- groupPages)
      yield {
        val clusterPages =  token.getService.getChildren(token.getToken, gPage.getId)
        //cluster pages e.g. LLDS_1_DEV
        for(cPage <- clusterPages
          if cPage.getTitle!="Archive")
          yield {
            val ksPages =  token.getService.getChildren(token.getToken, cPage.getId)
            //keyspaces pages e.g. LLDS_1_DEV - ftl
            for(ksPage <- ksPages)
              yield {
                //skip archive folder and the -TOKEN page
                if (
                  !ksPage.getTitle.toLowerCase.endsWith("token") &&
                  !ksPage.getTitle.toLowerCase.endsWith("metrics") &&
                  ksPage.getTitle.split(" - ").length==2) {
                  val ksName = ksPage.getTitle.split(" - ")(1).toLowerCase
                  val clusName = ksPage.getTitle.split(" - ")(0)
                  val x = new KeyspaceCluster (ksName,clusName)
                  keyspaceInfoList =  x :: keyspaceInfoList
                }
              }
          }
      }
    keyspaceInfoList
  }

  def getStringFromTable (content : Elem, name : String  ): String = {
    ( content \\ "tr" filter(x => x.child(0).text.contains(name) ) map (a => a.child(1).toString())).headOption.getOrElse("")
  }

  def getTextFromTable (content : Elem, name : String  ): String = {
    ( content \\ "tr" filter(x => x.child(0).text.contains(name) ) map (a => a.child(1).text)).headOption.getOrElse("")
  }

  def getUsersFromValue (token: Token, content : Elem, name : String): List[String] = {
    var retVal: List[String] = List.empty
    val rawUser = getStringFromTable(content, name)
    try {
      val test = scala.xml.XML.loadString("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "<body xmlns:ri=\"http://ri\">"+ rawUser + "</body>")

      retVal = (test \\ "user" ).map(a => token.getServiceLocator.getConfluenceserviceV2.getUserByKey(token.getToken, (a \ "@{http://ri}userkey").text).getFullname ).toList //.map(a => List(a))  //.foldLeft(List[Elem])((a,b) => a ++ b)
      logger.debug("retval: " + retVal)
    }
    catch {
      case e: Exception => {
        logger.error("EXCEPTION - getUsersFromValue - ", e)
      }
    }
    logger.debug(name + " " + retVal)
    retVal
  }

  def findLinkedKeyspace (content : Elem): String= {
    (content \\ "link" \\ "page" \ "@{http://ri}content-title" filter(a => a.text !=  "Patterns - Use Cases" )).text  //map(a => println(kPage.getTitle +" -> "+a.text))
  }

  def getKeyspaceInfo(token: Token, keyspaceName: String, space: String): KeyspaceInfo = {
    val p: Page = new Page
    val childPage = p.read(space,keyspaceName)

    //print("+++++++++++++++++++++++++++++++++++++++++++++++++ CHILDPAGE" + childPage.getContent.replaceAll("&","&amp;"));
    val content = scala.xml.XML.loadString("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      //need to removed nbsp and dashes from names
      //"<!DOCTYPE some_name [ \n<!ENTITY ndash \"&#160;\"> \n]> " +
      // body specify namespace to so that it can be used to extract URIs
      s"""<body xmlns:ri="http://ri">${childPage.getContent.replaceAll("&","&amp;")}</body>""")


    //find linked page!!
    val linkedKeyspace =findLinkedKeyspace(content)
    var applicationName: String  =null
    var dcab: String = null
    var dcabApprover = List[String]()
    var devContacts = List[String]()
    var opsContacts = List[String]()
    var productOwner = List[String]()
    var desc: String = null

    //if (kPage.getTitle == "api_directory") {
    if (linkedKeyspace =="") {
      println(keyspaceName)
      applicationName = getTextFromTable(content, "Name of Application/UseCase")
      dcab = getTextFromTable(content, "DCAB Group")
      dcabApprover = getUsersFromValue(token, content, "DCAB Approver")
      devContacts = getUsersFromValue(token, content, "Dev contacts(s)")
      opsContacts = getUsersFromValue(token, content, "Ops contact(s)")
      productOwner = getUsersFromValue(token, content, "Product Owner")
      desc = getTextFromTable( content, "Short Description of use-case")
    }
    else {
      logger.debug(keyspaceName +" -> "+ linkedKeyspace)
    }
    val keyspaceInfo = new KeyspaceInfo(keyspaceName, "", linkedKeyspace, dcab, dcabApprover, devContacts, opsContacts, productOwner, applicationName, desc)
    keyspaceInfo

  }


  def getKeyspacesInfoFromManualPage (token: Token, space: String): List[KeyspaceInfo] = {
    val pageName = "Keyspaces"
    val page: Page = new Page
    val parentPage: RemotePage = page.read(space, pageName)

    val keyspacePages =  token.getService.getChildren(token.getToken, parentPage.getId)

   var keyspaceInfoList : List[KeyspaceInfo] = List[KeyspaceInfo]()
    for(kPage <- keyspacePages)
      yield {
        keyspaceInfoList = getKeyspaceInfo (token, kPage.getTitle, space) :: keyspaceInfoList
      }
    keyspaceInfoList
  }


  def generateList (token: Token, space: String) : Unit = {

    //find all keyspaces
    val keyspaceInfoList = getKeyspaces (token)
    logger.debug(keyspaceInfoList.groupBy(_.keyspace).mapValues(_.map(_.clusterName)).toString())
    val keyspaceClusterList = keyspaceInfoList.groupBy(_.keyspace).mapValues(_.map(_.clusterName))
    val clusterKeyspaceList = keyspaceInfoList.groupBy(_.clusterName).mapValues(_.map(_.keyspace))
    logger.debug(keyspaceClusterList.toString())
    logger.debug(clusterKeyspaceList.toString())

    getKeyspacesInfoFromManualPage(token, space)
    println("---- FINISHED ---")
  }
}
