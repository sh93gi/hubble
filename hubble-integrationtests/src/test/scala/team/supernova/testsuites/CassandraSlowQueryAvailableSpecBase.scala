package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra._

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

abstract class CassandraSlowQueryAvailableSpecBase
  extends TestKit(ActorSystem("CassandraPerformanceSpec"))
    with FunSpecLike
    with ClusterConnectorFixture
    with CassandraClusterGroupFixture {

  def clusterInstance: ClusterEnv = clusterEnvWithSlowQueries

  /**
    * A ClusterEnv which has a dse_perf.node_slow_log table with some rows
    * Example value: cassandragroup.head.envs.head
 *
    * @return
    */
  def clusterEnvWithSlowQueries : ClusterEnv

  describe("Cassandra slow query analyzer") {
    it("should have slow query columnfamily") {
      new CassandraSlowQueryApi(clusterInstance).hasSlowQueryData() should be (true)
    }

    it("should find slow queries") {
      val all = ArrayBuffer[SlowQuery]()
      Try(new CassandraSlowQueryApi(clusterInstance).foreach(Some(50))(all.+=(_))) // For some high nr of days ago we will get timeouts
      all.size should be > 0
      all.size should be <= 50
    }

    it("rows should be transformable to slowqueries") {
      Try( // For some high nr of days ago we will get timeouts
        new CassandraSlowQueryApi(clusterInstance).foreach(Some(50))(queryDetails => {
          queryDetails.commands.size should be > 0
          queryDetails.duration should be > 0L
          queryDetails.keyspaces.size should be > 0
          queryDetails.tables.size should be > 0
        })
      )
    }
  }
}
