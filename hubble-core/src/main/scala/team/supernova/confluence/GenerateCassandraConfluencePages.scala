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
object GenerateCassandraConfluencePages {

  private def CONFLUENCE_HEADER (intro: String) = <ac:structured-macro ac:name="section">
      <ac:rich-text-body>
        <ac:structured-macro ac:name="column">
          <ac:parameter ac:name="width">50%</ac:parameter> <ac:rich-text-body>
          <h1>General information</h1>
          <ac:structured-macro ac:name="warning">
            <ac:parameter ac:name="title">GENERATED CODE!!!</ac:parameter>
            <ac:rich-text-body><p>NB!!! This page is generated based on information from Cassandra. PLEASE DON'T EDIT IT!!!</p></ac:rich-text-body>
          </ac:structured-macro>
          <p>{intro}</p>
        </ac:rich-text-body>
        </ac:structured-macro> <ac:structured-macro ac:name="column">
        <ac:parameter ac:name="width">50%</ac:parameter> <ac:rich-text-body>
          <ac:structured-macro ac:name="panel">
            <ac:parameter ac:name="title">What's on this page</ac:parameter> <ac:parameter ac:name="borderStyle">solid</ac:parameter> <ac:rich-text-body>
            <p>
              <ac:structured-macro ac:name="toc">
                <ac:parameter ac:name="maxLevel">4</ac:parameter>
              </ac:structured-macro>
            </p>
          </ac:rich-text-body>
          </ac:structured-macro>
        </ac:rich-text-body>
      </ac:structured-macro>
      </ac:rich-text-body>
    </ac:structured-macro>


  def generateKeyspacePage(keyspace: Keyspace, clusterInfo: ClusterInfo): String= {

    def generateTablePart (table: Table, k: Keyspace): String = {
      val size = table.columns.size.toString
      val possibleLinks = k.findPossibleLinks.filter(l => l.from.table_name.equals(table.table_name)).foldLeft(""){(a,p) => a + p.to.table_name + " on (" + p.on +")\n" }
      val queries = table.statements.foldLeft(""){(a,s) => a + s + "\n" }
      val tableWarnings = table.checks.filter(!_.hasPassed).map(_.details).sorted.mkString("\n")

      def whichColourClass(keyType: String): String = keyType match  {
        case "partition_key" => "highlight-green confluenceTd"
        case "clustering_key" => "highlight-blue confluenceTd"
        case _ =>"highlight-yellow confluenceTd"
      }

      def whichkeyType(keyType: String): String = keyType match  {
      case "partition_key" => " (pk)"
        case "clustering_key" => " (ck)"
        case _ =>""
      }

      //first row is needed due to rowSpan setting (see th element in table header)
      val firstRow =
        <tr>
          <td rowspan={size}>{table.table_name}</td>
          <td class={whichColourClass(table.columns.head.keyType)}>{ table.columns.head.column_name }{whichkeyType(table.columns.head.keyType)}</td>
          <td class={whichColourClass(table.columns.head.keyType)}>{ table.columns.head.dataTypeLong}</td>
          <td rowspan={size}>
            { Confluence.confluenceCodeBlock("CQL",table.cql,"sql")}
            { Confluence.confluenceCodeBlock("Queries",queries,"sql")}
            { Confluence.confluenceCodeBlock("References",possibleLinks,"none")}
            { Confluence.confluenceCodeBlock("Comments",table.comments,"none")}
            { Confluence.confluenceCodeBlock("Warnings",tableWarnings,"none")}
            { clusterInfo.slowQueries.tableSlow.get(s"${keyspace.keyspace_name}.${table.table_name}").map(topSlowest => slowQueryTable(topSlowest.get(10))).getOrElse("")}
          </td>
        </tr>

      val restRows = table.columns.tail.foldLeft(""){(a,c) => a +
        <tr>
          <td class={whichColourClass(c.keyType)}>{ c.column_name }{whichkeyType(c.keyType)}</td>
          <td class={whichColourClass(c.keyType)}>{ c.dataTypeLong}</td>
        </tr>
      }
      firstRow + restRows
    }

    val keyspaceWarnings = keyspace.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.WARNING)).map(_.details).sorted.mkString("\n")
    val keyspaceErrors = keyspace.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.ERROR)).map(_.details).sorted.mkString("\n")

    //The actual page itself
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>{CONFLUENCE_HEADER("This section summarises all the keyspace information.")}<hr/>
      <h1>Keyspace: {keyspace.keyspace_name}</h1>
      <p>{ Confluence.confluenceCodeBlock("Errors", keyspaceErrors ,"none")}
        { Confluence.confluenceCodeBlock("Warnings", keyspaceWarnings ,"none")}
        { clusterInfo.slowQueries.keyspaceSlow.get(keyspace.keyspace_name).map(topSlowest => slowQueryTable(topSlowest.get(10))).getOrElse("")}
      </p>
      <p>{ Confluence.confluenceCodeBlock("Schema",keyspace.schemaScript,"none")}</p>
      <h1>Tables</h1>
      <p>
        <table>
          <tbody><tr><th>Table Name</th><th colspan="2">Columns</th><th>Extras</th></tr>
            {scala.xml.Unparsed( keyspace.tables.foldLeft("") { (at, table) => at +  generateTablePart (table, keyspace)} )}
          </tbody>
        </table>
      </p>
    </body>.toString()
  }

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
              { clusterInfo.slowQueries.keyspaceSlow.get(k.keyspace_name).map(topSlowest => slowQueryTable(topSlowest.get(10))).getOrElse("")}
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
    val clusterGraphUrl = new StringTemplate(clusterInfo.cluster.graphite_template).fillWith(clusterInfo.cluster.graphite)

    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import org.json4s.jackson.Serialization.write
    implicit val formats = DefaultFormats

    //The actual cluster page itself
    //need <body> tag otherwise ArrayBuilder is shown on confluence
      <body>{CONFLUENCE_HEADER("This section summarises all the cluster information.")}<hr/>
        <h1>Cluster: {clusterInfo.cluster_name}</h1>
        <p>{ Confluence.confluenceCodeBlock("Errors", clusterErrors ,"none")}
          { Confluence.confluenceCodeBlock("Warnings", clusterWarnings ,"none")}
        </p>
          <p><a href="{clusterGraphUrl}"><img src={ clusterGraphUrl }/></a></p>
        <p>{
          val slowToShow = clusterInfo.slowQueries.clusterSlow.get(10)
            if (slowToShow.isEmpty)
              ""
                else slowQueryTable(clusterInfo.slowQueries.clusterSlow.get(10))}</p>
        <p>{slowQueryFailures(clusterInfo.slowQueries)}</p>
        <h1>Host Information</h1>
        <p>
          <table>
            <tbody><tr><th>Data Center</th><th>Host Name</th><th>IP Address</th><th>Rack</th><th>C* Version</th><th>Extras</th></tr>
              {scala.xml.Unparsed( clusterInfo.hosts.to[SortedSet].foldLeft("") { (at, host) => at +
                                            <tr>
                                              <td>{host.dataCenter}</td>
                                              <td>{host.canonicalHostName}</td>
                                              <td>{host.ipAddress}</td>
                                              <td>{host.rack}</td>
                                              <td>{host.version}</td>
                                              <td>{
                                                val yaml  =  try { pretty(parse( write(host.opsCenterNode.head.cassandra ) ))}
                                                 catch {case e: Exception => ""}
                                                Confluence.confluenceCodeBlock("Yaml",yaml  ,"none")}</td>
                                            </tr>
                                            })
              }
            </tbody>
          </table>
        </p>
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


  //TODO - make graphite checker!!!  Seems to be rather messy
   //this is using graphana!
  def clusterGraphite(cluster_name: String, graphana_host: String ) = <a href={s"http://$graphana_host/dashboard/db/cluster-health-per-cluster?Cluster=$cluster_name"}>Overview</a>

  def generateClusterGroupPage(groupClusters: GroupClusters, project: String): String=  {

    val listKeyspace: SortedSet[String] = groupClusters.clusterInfoList.flatMap(_.keyspaces).map(_.keyspace_name)

    //this must not be sorted as it is already sorted - just need the list of names!
    val listClusterName: List[String] = groupClusters.clusterInfoList.foldLeft(List[String]()){(a,b) => a ++ List(b.cluster_name)}
    println (s"Cluster List: $listClusterName")

    def whichColourClassBoolean(isTrue: Boolean): String =  if (isTrue) {"highlight-green confluenceTd"} else {"highlight-red confluenceTd"}

    <body>{CONFLUENCE_HEADER("This section briefly summarises all the clusters information.")}<hr/>
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



  //TODO create Cluster page if not exits and group page if does not exist!
  //TODO CHeck if gets updated the first ever time
    def generateGroupConfluencePages ( allClusters    : GroupClusters,
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
    parentPage.setContent( s"<body>${generateClusterGroupPage(allClusters, project)}</body>")
    page.store(parentPage)

    var tokenContent: String = ""

    //Per ClusterInfo - create page
    for(clusterInfo <- allClusters.clusterInfoList)
      yield {
        val clusterPageName = clusterInfo.cluster_name.toUpperCase
        //create the specific summary cluster page
        tokenContent = tokenContent +"<br/>"+ Confluence.confluenceCreatePage (project,clusterPageName, generateClusterSummaryPage(project, clusterInfo), page, parentPage, tokenPage )
        val clusterParentPage: RemotePage = page.read(project,clusterPageName)

        //Per keyspace create pages
        for(keyspace <- clusterInfo.keyspaces)
          yield {
            val content: String = generateKeyspacePage(keyspace, clusterInfo)
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
