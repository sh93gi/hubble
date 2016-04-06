package team.supernova.cassandra

import java.net.InetAddress

import com.datastax.driver.core.exceptions.ReadTimeoutException
import com.sun.mail.iap.ConnectionException
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import team.supernova.{retryExecute, using}

import scala.collection.JavaConverters._

class CassandraSlowQueryApi(cluster: ClusterEnv) {
  val log =LoggerFactory.getLogger(classOf[CassandraSlowQueryApi])
  def newSession() = new ClusterEnvConnector(cluster).connect()

    def hasSlowQueryData(): Boolean = {
      using(newSession()){
        session =>{
          try{
          val dsePerformanceCount = retryExecute(
            {session.execute("select count(*) from system.schema_columnfamilies where keyspace_name='dse_perf' and columnfamily_name='node_slow_log';").asScala},
            3)
            return dsePerformanceCount.head.getLong(0)>0
          }catch{
            case e: ConnectionException => return false
          }
        }
      }
    }

  def foreach(limit: Option[Int])(op: (SlowQuery)=>Unit) : Unit = {
    if(!hasSlowQueryData()){
      return
    }
    val limit_text = limit.map("limit %d".format(_)).getOrElse("")
    var count = 0
    using(newSession()) { session =>
      if (limit.isDefined && count>=limit.get)
        return
      val nodes = session.execute("select peer from system.peers;").all().asScala.map(_.getInet("peer"))
      var failed : Option[Throwable] = None
      for (offset <- 0 to 7){
        val midnight = CassandraDateTime.toMidnight(new DateTime().minusDays(offset)).toDate
        for (node_ip <- nodes){
          log.info(s"Getting slow queries for node $node_ip and date $midnight")
          val node_inet = InetAddress.getByName(node_ip.getHostAddress)
          if (limit.isDefined && count>=limit.get)
            return //We are done, no more nodes/days to try
          val query = session.prepare(s"select commands, table_names, duration from dse_perf.node_slow_log " +
            s"WHERE node_ip=? " +
            s"AND date=? " +
            s"$limit_text;")
          val statement = query.bind(node_inet, midnight)
          try{
            // Could timeout on retrieving the first page
            val results = retryExecute(
              {session.execute(statement).asScala},
              3)
            // Foreach can time out on getting the next page
            results.foreach(r=>{
              count+=1
              if (limit.isEmpty || count<=limit.get)
                op(SlowQuery(r))
            })
          }catch {
            case e:ReadTimeoutException => failed=Some(e)
          }
        }
      }
      // After all requested days for all available nodes, throw exception if not all data could be retrieved
      failed.foreach(throw _) //Rethrow exception if at least one occurred
    }
  }
}