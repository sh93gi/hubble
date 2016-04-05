package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraSlowQueryApi, ClusterEnv, SlowQuery}

import scala.collection.mutable.ArrayBuffer

class CassandraSlowQuerySpec
  extends TestKit(ActorSystem("CassandraPerformanceSpec"))
    with FunSpecLike
    with ClusterConnectorFixture
    with CassandraClusterGroupFixture {
  val clusterInstance: ClusterEnv = cassandragroup.head.envs.head // one with dse_perf keyspace

  describe("Cassandra slow query analyzer") {
    it("should have slow query columnfamily") {
      new CassandraSlowQueryApi(clusterInstance).hasSlowQueryData() should be (true)
    }

    it("should find slow queries") {
      val all = ArrayBuffer[SlowQuery]()
      new CassandraSlowQueryApi(clusterInstance).foreach(Some(50))(all.+=(_))
      all.size should be > 1
    }

    it("rows should be transformable to slowqueries") {
      using(newSession()) { session =>
        new CassandraSlowQueryApi(clusterInstance).foreach(Some(50))(queryDetails => {
          queryDetails.commands.size should be > 0
          queryDetails.duration should be >= 0L
          queryDetails.keyspaces.size should be > 0
          queryDetails.tables.size should be > 0
        })
      }
    }
  }
}
