package team.supernova.cassandra

import java.net.InetAddress

import com.datastax.driver.core.Session
import com.datastax.driver.core.exceptions.{QueryTimeoutException, ReadTimeoutException, UnauthorizedException}
import com.sun.mail.iap.ConnectionException
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import team.supernova.{retryExecute, using}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class CassandraSlowQueryApi(cluster: ClusterEnv) {
  val log = LoggerFactory.getLogger(classOf[CassandraSlowQueryApi])

  private def newSession() = new ClusterEnvConnector(cluster).connect()

  def hasSlowQueryData(): Boolean = {
    using(newSession()) {
      session => {
        hasSlowQueryData(session)
      }
    }
  }

  private def hasSlowQueryData(session: Session): Boolean = {
    try {
      val query = "select count(*) from system.schema_columnfamilies where keyspace_name='dse_perf' and columnfamily_name='node_slow_log';"
      val dsePerformanceCount = retryExecute(
        retries = 3,
        command = session.execute(query).asScala
      )
      if (dsePerformanceCount.head.getLong(0)==0)
        return false
      queryNodeSlowLog(session, Some(1), _=>{})  // Verify if table can be queried without exceptions (permissions)
      return true
    } catch {
      case e: QueryTimeoutException =>
        log.warn(s"Got timeout querying system.schema_columnfamilies: ${e.getMessage}")
        return false
      case e: ConnectionException =>
        log.warn(s"Got connectionexception while determining availability of node_slow_log: ${e.getMessage}")
        return false
      case e: UnauthorizedException =>
        log.warn(s"The dse_perf.node_slow_log exists but the user used by hubble is unauthorized to read from it: ${e.getMessage}")
        return false
    }
  }

  def foreach(op: (SlowQuery) => Unit): Unit = foreach(None)(op)

  def foreach(limit: Int)(op: (SlowQuery) => Unit): Unit = foreach(Some(limit))(op)

  def foreach(limit: Option[Int])(op: (SlowQuery)=>Unit) : Unit = {
    using(newSession()) { session =>
      if(hasSlowQueryData(session)) queryNodeSlowLog(session, limit, op)
    }
  }

  def queryNodeSlowLog(session: Session, limit: Option[Int], op: (SlowQuery) => Unit): Unit = {
    val limit_text = limit.map("limit %d".format(_)).getOrElse("")
    var count = 0
    if (limit.isDefined && count >= limit.get)
      return
    val query = session.prepare(s"select commands, table_names, duration from dse_perf.node_slow_log " +
      s"WHERE node_ip=? " +
      s"AND date=? " +
      s"$limit_text;")
    val nodes = getNodes(session)
    var failed: Option[Throwable] = None
    for (offset <- 0 to 7) {
      val midnight = CassandraDateTime.toMidnight(new DateTime().minusDays(offset)).toDate
      for (node_ip <- nodes) {
        log.info(s"Getting slow queries for node $node_ip and date $midnight")
        val node_inet = InetAddress.getByName(node_ip.getHostAddress)
        if (limit.isDefined && count >= limit.get)
          return //We are done, no more nodes/days to try
        val statement = query.bind(node_inet, midnight)
        try {
          // Could timeout on retrieving the first page
          val results = retryExecute(
            {
              session.execute(statement).asScala
            },
            3)
          // Foreach can time out on getting the next page
          results.foreach(r => {
            count += 1
            if (limit.isEmpty || count <= limit.get)
              op(SlowQuery(r))
          })
        } catch {
          case e: ReadTimeoutException => failed = Some(e)
        }
      }
    }
    // After all requested days for all available nodes, throw exception if not all data could be retrieved
    failed.foreach(throw _) //Rethrow exception if at least one occurred
  }
}

  def getNodes(session: Session): Seq[InetAddress] = {
    session.execute("select peer from system.peers;").all().asScala.map(_.getInet("peer"))
  }
}
