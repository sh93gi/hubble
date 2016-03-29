package team.supernova

import com.datastax.driver.core._
import team.supernova.cassandra.{ClusterEnv, ClusterSlowQueries, OpsCenterApi}
import team.supernova.checks._

import scala.collection.JavaConversions._
import scala.collection.SortedSet

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */
object checks {
  def combineChecks(acc: List[Check], checkable: List[Checkable]): List[Check] =
    acc ++ checkable.flatMap(_.checks)
}

object StringValidation {
  val alphaNumericLowercase=(('a' to 'z') ++ ('0' to '9') ++ "_").toSet
  val alphaNumeric=(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "_").toSet
  val alpha=(('a' to 'z') ++ ('A' to 'Z')).toSet
  def isAlphaNumericLowercase(s:String)=s.forall(alphaNumericLowercase.contains(_))
  def isAlphaNumeric(s:String)=s.forall(alphaNumeric.contains(_))
  def startsWithLetter(s:String)=s match {
    case "" => false
    case _ => s.head.toString.forall(alpha.contains(_))
  }
  def isNotTooLong(str: String): Boolean=str.size<32
  def isNotABitLong(str: String): Boolean=str.size<24
}

case class Check(name: String, details: String, hasPassed: Boolean, severity: String)

trait Checkable {
  def checks: List[Check] =
    combineChecks(myChecks, children)

  protected def myChecks: List[Check]

  protected def children: List[Checkable]
}

case class Column(columnMetadata: ColumnMetadata, keyType: String) extends Checkable {
  val keyspace_name = columnMetadata.getTable.getKeyspace
  val table_name = columnMetadata.getTable.getName
  val column_name = columnMetadata.getName

  val dataType = columnMetadata.getType.getName.toString match {
    case "list" => s"list <${columnMetadata.getType.getTypeArguments.get(0)}>"
    case "map" => s"map <${columnMetadata.getType.getTypeArguments.get(0)}, ${columnMetadata.getType.getTypeArguments.get(1)}>"
    case "set" => s"set <${columnMetadata.getType.getTypeArguments.get(0)}>"
    case _ => columnMetadata.getType.getName.toString
  }
  val dataTypeLong = dataType + (if (columnMetadata.isStatic) {
    " STATIC"
  } else {
    ""
  })
  val index_name = if (columnMetadata.getIndex != null) {
    columnMetadata.getIndex.getName
  } else {
    ""
  }

  val myChecks: List[Check] = {
    List(
      Check("Secondary Index exists", s"$index_name index on table $table_name.$column_name!", index_name == "", "warning"),
      Check("ColumnName begin letter", s"$table_name.$column_name is not beginning with a letter.", StringValidation.startsWithLetter(column_name), "warning"),
      Check("ColumnName length check", s"Column name $table_name.$column_name is too long", StringValidation.isNotTooLong(column_name), "warning"),
      Check("ColumnName length warning", s"Column name $table_name.$column_name is a bit long", StringValidation.isNotABitLong(column_name), "info"),
      Check("ColumnName check", s"Column name $table_name.$column_name is not according to the naming conventions", StringValidation.isAlphaNumericLowercase(column_name), "warning")
    )
  }
  val children = List.empty
}

case class Table(tableMetadata: TableMetadata) extends Checkable  with Ordered [Table] {

  def compare (that: Table) = {
    this.table_name.compareTo(that.table_name)
  }

  val keyspace_name = tableMetadata.getKeyspace.getName
  val table_name = tableMetadata.getName
  val comments = tableMetadata.getOptions.getComment
  val cql = tableMetadata.exportAsString  //this includes indexes
  val pkColumns: List[Column] = tableMetadata.getPartitionKey.foldLeft(List[Column]()) { (a, p) => a ++ List(Column(p, "partition_key")) }
  val ckColumns: List[Column] = tableMetadata.getClusteringColumns.foldLeft(List[Column]()) { (a, p) => a ++ List(Column(p, "clustering_key")) }
  val regularColumns: List[Column] = tableMetadata.getColumns.filter(c => !tableMetadata.getPrimaryKey.contains(c)).foldLeft(List[Column]()) { (a, p) => a ++ List(Column(p, "regular")) }
  val columns = {
    pkColumns ++ ckColumns ++ regularColumns
  }

  val insertStatement: String = {
    val colList = columns.foldLeft("") { (a, column) => a + (if (!a.isEmpty) ", " else "") + column.column_name }
    val valuesList = columns.foldLeft("") { (a, column) => a + (if (!a.isEmpty) ", " else "") + "?" }
    s"INSERT INTO $table_name ($colList) VALUES ($valuesList);"
  }

  val deleteStatements = ckColumns.inits.map(pkColumns ++ _).toList.foldLeft(List[String]()) { (acc, col) =>
    val whereList = col.foldLeft("") { (a, col2) => a + (if (!a.isEmpty) " AND " else "") + col2.column_name + " = ?" }
    //println (whereList)
    val delStmt = s"DELETE FROM $table_name WHERE $whereList;"
    acc ++ List(delStmt)
  }

  val selectStatements = ckColumns.inits.map(pkColumns ++ _).toList.foldLeft(List[String]()) { (acc, col) =>
    val whereList = col.foldLeft("") { (a, col2) => a + (if (!a.isEmpty) " AND " else "") + col2.column_name + " = ?" }
    //println (whereList)
    val selStmt: String = s"SELECT * FROM $table_name WHERE $whereList;"
    acc ++ List(selStmt)
  }

  val statements = {
    selectStatements ++ List(insertStatement) ++ deleteStatements
  }

  // TODO extra table checks based on properties
  val myChecks: List[Check] = List(
    Check("Single column table", s"$table_name only has one column!", columns.size != 1, "warning"),
    Check("TableName begin letter", s"$table_name is not beginning with a letter.", StringValidation.startsWithLetter(table_name), "warning"),
    Check("TableName length check", s"Table name ${table_name} is too long", StringValidation.isNotTooLong(table_name), "warning"),
    Check("TableName length warning", s"Table name ${table_name} is a bit long", StringValidation.isNotABitLong(table_name), "info"),
    Check("TableName check", s"Table name ${table_name} is not according to the naming conventions", StringValidation.isAlphaNumericLowercase(table_name), "warning")
  )

  val children = columns
}

case class Link(from: Table, to: Table, on: String)

case class Keyspace(  keyspaceMetaData: KeyspaceMetadata,
                      private val validDCnames: SortedSet[String]
                     )
  extends Checkable with Ordered[Keyspace] {


  def compare(that: Keyspace): Int = this.keyspace_name.toLowerCase compare that.keyspace_name.toLowerCase

  val keyspace_name = keyspaceMetaData.getName
  // TODO fix so that it is the entire schema
  val schemaScript = keyspaceMetaData.asCQLQuery()  //only contains the keyspace
  val tables: List[Table] = keyspaceMetaData.getTables.foldLeft(List[Table]()) { (a, t) => a ++ List(Table(t)) }.sorted
  // TODO make more elegant!
  val dataCenter: SortedSet[String] = keyspaceMetaData.getReplication.filterNot(a => a._1.equals("class")).filterNot(b => b._1.equals("replication_factor")).map(_._1).to

  //for each table check if link (pks) exists in another table
  val findPossibleLinks: List[Link] = tables.foldLeft(List(): List[Link]) { (acc, t) =>
    acc ++ tables.filter(a => a.table_name != t.table_name).
      foldLeft(List(): List[Link]) { (a1, t1) =>
      val a = t1.columns.map(_.column_name).toSet
      val checkLink = a ++ t.pkColumns.map(_.column_name) == a //if we add the list of pk to a set and the set remains the same then we have a potential link!
      //val s = s" Link ${t1.table_name} to ${t.table_name} on (${t.pkColumns.foldLeft(""){(a, col) => a + (if (!a.isEmpty ) ", " else "") + col.column_name }}) $checkLink"
      if (checkLink) {
        val l = new Link(t1, t, t.pkColumns.foldLeft("") { (a, col) => a + (if (!a.isEmpty) ", " else "") + col.column_name })
        a1 ++ List(l)
      }
      else
        a1
    }
  }

  val ignoreKeyspaces: Set[String] = Set("system_auth", "system_traces", "system", "system_admin", "dse_system", "dse_security", "dse_perf", "solr_admin")
  val dcNames = dataCenter.foldLeft("") { (a, w) => a + " '" + w + "'" }

  val myChecks: List[Check] = List(
    // TODO check DC names are valid
    Check("Keyspace is unused check", s"No tables in keyspace: $keyspace_name!", tables.size > 0, "warning"),
    // TODO when CassandraErrors is completely removed from the supernova driver when this message may change again
    Check("CassandraErrors table check", s"$keyspace_name contains CassandraErrors table. This is not mandatory anymore.", ignoreKeyspaces.contains(keyspace_name) || tables.count(t => t.table_name.equals("cassandraerrors")) == 1, "info"),
    Check("ModelMutation table check", s"$keyspace_name has no ModelMutation table. Are you using Nolio for setting your cassandra data model?", ignoreKeyspaces.contains(keyspace_name) || tables.count(t => t.table_name.equals("modelmutation")) != 0, "info"),
    Check("Data Center names check", s"$keyspace_name has incorrect DC names:${dcNames}", validDCnames ++ dataCenter == validDCnames, "error"),
    Check("KeyspaceName length check", s"Keyspace name ${keyspace_name} is too long", StringValidation.isNotTooLong(keyspace_name), "warning"),
    Check("KeyspaceName length warning", s"Keyspace name ${keyspace_name} is a bit long", StringValidation.isNotABitLong(keyspace_name), "info"),
    Check("KeyspaceName check", s"Keyspace name ${keyspace_name} is not according to the naming conventions", StringValidation.isAlphaNumericLowercase(keyspace_name), "warning")
  )
  val children = tables
}

case class NodeHost(host: Host, opsCenterNode: Option[OpsCenterNode]) extends Ordered[NodeHost] {

  def compare(that: NodeHost): Int = this.dataCenter.concat(this.canonicalHostName) compare that.dataCenter.concat(that.canonicalHostName)

  val ipAddress = host.getAddress.getHostAddress
  val version = host.getCassandraVersion
  val dataCenter = host.getDatacenter
  val canonicalHostName = host.getAddress.getCanonicalHostName
  val rack = host.getRack
  // TODO add opsCenter Info and warnings!
}

case class ClusterInfo(val metaData: Metadata,
                       val slowQueries: ClusterSlowQueries,
                       val opsCenterClusterInfo: Option[OpsCenterClusterInfo],
                       cluster: ClusterEnv, group: String)
  extends Checkable with Ordered[ClusterInfo] {

  val cluster_name = metaData.getClusterName
  val schemaAgreement = metaData.checkSchemaAgreement()
  val dataCenter: SortedSet[String] = metaData.getAllHosts.groupBy(h => h.getDatacenter).keys.to
  val keyspaces: SortedSet[Keyspace] = metaData.getKeyspaces.map(i => {
    new Keyspace(i, dataCenter)
  }).to

  val hosts = metaData.getAllHosts.map(h => new NodeHost(h, opsCenterClusterInfo.flatMap(a => a.nodes.find(n => n.name.equals(h.getSocketAddress.getAddress.getHostAddress)))))
  // TODO add cluster checks summary  ie check DC names etc!
  // TODO implement compare keyspaces - one cluster to another

  val myChecks: List[Check] = List(
    Check("Cluster agreement check", s"$cluster_name schema agreement issues!", schemaAgreement, "error")
  )

  val children = keyspaces.toList

  def compare(that: ClusterInfo): Int = {
    println (s"${this.cluster_name} compared to ${that.cluster_name}  = ${(this.cluster.sequence.toString compare that.cluster.sequence.toString) + this.group compare that.group} ")
    (this.cluster.sequence.toString compare that.cluster.sequence.toString) + this.group compare that.group
  }

}

case class GroupClusters(clusterInfoList: SortedSet[ClusterInfo]) {

  val checks: List[Check] = {
    val clusterChecks = List(
      // TODO check naming conventions of DC names
      Check("Check DC names TODO", s"FIXME!", true, "warning")
    )
    clusterInfoList.foldLeft(clusterChecks) { (acc, clust) => acc ++ clust.checks }
  }
}
