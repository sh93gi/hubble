package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra._

import scala.collection.mutable.ArrayBuffer

abstract class CassandraSlowQueryUnavailableSpecBase
  extends TestKit(ActorSystem("CassandraPerformanceSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture {

  val clusterInstance: ClusterEnv = clusterEnvWithSlowQueries

  /**
    * A ClusterEnv which has NOT a dse_perf.node_slow_log table
    * This SpecBase verifies that the CassandraSlowQueryApi can deal with missing dse_perf.node_slow_log or insufficient rights
    * Example value: cassandragroup.head.envs.last
    * @return
    */
  def clusterEnvWithSlowQueries : ClusterEnv

  describe("Cassandra slow query analyzer on cluster without dse_perf keyspace") {
    it("should NOT have slow query columnfamily") {
      new CassandraSlowQueryApi(clusterInstance).hasSlowQueryData() should be (false)
    }


    it("should NOT find slow queries") {
      val all = ArrayBuffer[SlowQuery]()
      new CassandraSlowQueryApi(clusterInstance).foreach(None)(all.+=(_))
      all.size should be (0)
    }
  }
}
