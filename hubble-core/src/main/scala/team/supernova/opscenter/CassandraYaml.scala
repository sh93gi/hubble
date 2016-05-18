package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

/**
  * Parse cassandra yaml content string
  */
object CassandraYaml {
 def parseBody (body: String) : CassandraYaml = {
  implicit val formats = DefaultFormats
  CassandraYaml(parse(body).extract[Map[String, Any]])
 }
}

/**
  * Parsed cassandra yaml file
  *
  * @param all : the yaml file, parsed as Map to be able to list all elements
  */
case class CassandraYaml(val all: Map[String, Any]){

 lazy val authenticator: Option[String] = all.get("authenticator").map(_.asInstanceOf[String])
 lazy val authority : Option[String] = all.get("authority").map(_.asInstanceOf[String])
 lazy val auto_snapshot : Option[Boolean] = all.get("auto_snapshot").map(_.asInstanceOf[Boolean])
 lazy val cluster_name : Option[String] = all.get("cluster_name").map(_.asInstanceOf[String])
 lazy val concurrent_reads : Option[Int] = all.get("concurrent_reads").map(_.asInstanceOf[Int])
 lazy val concurrent_writes : Option[Int] = all.get("concurrent_writes").map(_.asInstanceOf[Int])
}
