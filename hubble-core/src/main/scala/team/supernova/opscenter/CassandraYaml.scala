package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}

/**
  * Parse cassandra yaml content string
  */
object CassandraYaml {
 def parseBody (body: String) : CassandraYaml = {
  implicit val formats = DefaultFormats
  //parse(body).extract[CassandraYaml]
  CassandraYaml(parse(body).extract[Map[String, Any]])
 }
}

/**
  * Parsed cassandra yaml file
  *
  * @param all : the yaml file, parsed as Map to be able to list all elements
  */
case class CassandraYaml(all: Map[String, Any]){

 def authenticator: Option[String] = all.get("authenticator").map(_.asInstanceOf[String])
 def authority : Option[String] = all.get("authority").map(_.asInstanceOf[String])
 def auto_snapshot : Option[Boolean] = all.get("auto_snapshot").map(_.asInstanceOf[Boolean])
 def cluster_name : Option[String] = all.get("cluster_name").map(_.asInstanceOf[String])
 def concurrent_reads : Option[Int] = all.get("concurrent_reads").map(_.asInstanceOf[Int])
 def concurrent_writes : Option[Int] = all.get("concurrent_writes").map(_.asInstanceOf[Int])
}
