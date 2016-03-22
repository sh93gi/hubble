package team.supernova.cassandra

import com.datastax.driver.core.Row

import scala.collection.JavaConverters._

case class SlowQuery(commands: List[String], tables: Set[String], keyspaces: Set[String], duration:Long){

  override def toString = "SlowQuery(commands=%s, tables=%s, keyspaces=%s, duration=%d".format(
    commands.toString(),
    tables.toString(),
    keyspaces.toString(),
    duration)
}

object SlowQuery{

  def getKeyspace(tableName: String): String ={
    tableName.split('.').head
  }

  def apply(r: Row): SlowQuery = {
    SlowQuery(r.getList("commands", classOf[String]).asScala.toList,
      r.getSet("table_names", classOf[String]).asScala.toSet,
      r.getSet("table_names", classOf[String]).asScala.map(getKeyspace).toSet,
      r.getLong("duration"))
  }

  def slowest(set: Seq[SlowQuery]) : SlowQuery = {
    set.sortBy(-_.duration).head
  }

}

