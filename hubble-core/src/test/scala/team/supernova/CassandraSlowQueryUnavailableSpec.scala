package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraSlowQueryApi, ClusterEnv, SlowQuery}

import scala.collection.mutable.ArrayBuffer

class CassandraSlowQueryUnavailableSpec
  extends TestKit(ActorSystem("CassandraPerformanceSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture {

  val clusterInstance: ClusterEnv = cassandragroup.head.envs.last // one without dse_perf keyspace

  describe("Cassandra slow query analyzer on cluster without dse_perf keyspace") {
    ignore("all clusters have dse_perf now")(it("should NOT have slow query columnfamily") {
      new CassandraSlowQueryApi(clusterInstance).hasSlowQueryData() should be (false)
    }
    )

    ignore("all clusters have dse_perf now")(it("should NOT find slow queries") {
      val all = ArrayBuffer[SlowQuery]()
      new CassandraSlowQueryApi(clusterInstance).foreach(None)(all.+=(_))
      all.size should be (0)
    }
    )
  }
}
