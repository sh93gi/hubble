package team.supernova.cassandra

import java.net.InetAddress

import com.datastax.driver.core.Session
import com.datastax.driver.core.exceptions.ReadTimeoutException
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
      dsePerformanceCount.head.getLong(0) > 0
    } catch {
      case e: ConnectionException => {
        log.warn("Got error trying to retrieve slow log data", e)
        false
      }
    }
  }

  def foreach(op: (SlowQuery) => Unit): Unit = foreach(None)(op)

  def foreach(limit: Int)(op: (SlowQuery) => Unit): Unit = foreach(Some(limit))(op)

  private def foreach(limit: Option[Int])(op: (SlowQuery) => Unit): Unit = {
    using(newSession()) { session =>
      if (hasSlowQueryData(session)) {
        val limit_text = limit.map("limit %d".format(_)).getOrElse("")
        var count = 0

        if (limit.isDefined && count >= limit.get)
          return

        lazy val query = session.prepare(s"select commands, table_names, duration from dse_perf.node_slow_log " +
          s"WHERE node_ip=? " +
          s"AND date=? " +
          s"$limit_text;")

        lazy val nodes = getNodes(session)

        val failed: ListBuffer[Throwable] = ListBuffer()
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
                command = session.execute(statement).asScala,
                retries = 3
              )
              // Foreach can time out on getting the next page
              results.foreach(r => {
                count += 1
                if (limit.isEmpty || count <= limit.get)
                  op(SlowQuery(r))
              })
            } catch {
              case e: ReadTimeoutException => failed += e
            }
          }
        }
        // After all requested days for all available nodes, throw exception if not all data could be retrieved
        failed.foreach(throw _) //Rethrow exception if at least one occurred
      }
    }
  }

  def getNodes(session: Session): Seq[InetAddress] = {
    session.execute("select peer from system.peers;").all().asScala.map(_.getInet("peer"))
  }
}
