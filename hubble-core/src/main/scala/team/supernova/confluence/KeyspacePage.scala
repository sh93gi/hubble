package team.supernova.confluence

import team.supernova._
import team.supernova.{Table, ClusterInfo, Keyspace}

import scala.xml.NodeSeq

object KeyspacePage {


  def generateKeyspacePage(keyspace: Keyspace, clusterInfo: ClusterInfo): String= {

    val keyspaceWarnings = keyspace.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.WARNING)).map(_.details).sorted.mkString("\n")
    val keyspaceErrors = keyspace.checks.filterNot(_.hasPassed).filter(c => c.severity.equals(Severity.ERROR)).map(_.details).sorted.mkString("\n")

    //The actual page itself
    //need <body> tag otherwise ArrayBuilder is shown on confluence
    <body>{Confluence.CONFLUENCE_HEADER("This section summarises all the keyspace information.")}<hr/>
      <h1>Keyspace: {keyspace.keyspace_name}</h1>
      <p>{ Confluence.confluenceCodeBlock("Errors", keyspaceErrors ,"none")}
        { Confluence.confluenceCodeBlock("Warnings", keyspaceWarnings ,"none")}
        { clusterInfo.slowQueries.keyspaceSlow.get(keyspace.keyspace_name).map(topSlowest => SlowQuerySections.slowQueryTable(topSlowest.get(10))).getOrElse("")}
      </p>
      <p>{ Confluence.confluenceCodeBlock("Schema",keyspace.schemaScript,"none")}</p>
      <h1>Tables</h1>
      <p>
        <table>
          <tbody><tr><th>Table Name</th><th colspan="2">Columns</th><th>Extras</th></tr>
            {scala.xml.Unparsed( keyspace.tables.foldLeft("") { (at, table) => at +  generateTablePart (table, keyspace, clusterInfo)} )}
          </tbody>
        </table>
      </p>
    </body>.toString()
  }


  def generateTablePart (table: Table, keyspace: Keyspace, clusterInfo: ClusterInfo): String = {
    val size = table.columns.size.toString
    val possibleLinks = keyspace.findPossibleLinks.filter(l => l.from.table_name.equals(table.table_name)).foldLeft(""){ (a, p) => a + p.to.table_name + " on (" + p.on +")\n" }
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
          { clusterInfo.slowQueries.tableSlow.get(s"${keyspace.keyspace_name}.${table.table_name}").map(topSlowest => SlowQuerySections.slowQueryTable(topSlowest.get(10))).getOrElse("")}
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
}
