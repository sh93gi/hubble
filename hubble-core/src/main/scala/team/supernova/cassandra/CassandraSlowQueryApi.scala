package team.supernova.cassandra

import com.sun.mail.iap.ConnectionException
import team.supernova.{retryExecute, using}

import scala.collection.JavaConverters._

class CassandraSlowQueryApi(cluster: ClusterEnv) {
  def newSession() = new ClusterEnvConnector(cluster).connect()

    def hasSlowQueryData(): Boolean = {
      using(newSession()){
        session =>{
          try{
          val dsePerformanceCount = retryExecute(
            {session.execute("select count(*) from  system.schema_columnfamilies where keyspace_name='dse_perf' and columnfamily_name='node_slow_log';").asScala},
            3)
            return dsePerformanceCount.head.getLong(0)>0
          }catch{
            case e: ConnectionException => return false
          }
        }
      }
    }

  def foreach(op: (SlowQuery)=>Unit) : Unit = {
    if(!hasSlowQueryData()){
      return
    }
    using(newSession()) { session =>
      val results = retryExecute(
        {session.execute("select * from dse_perf.node_slow_log limit 50;").asScala},
        3)
      results.foreach(r=>op(SlowQuery(r)))
    }
  }
}
