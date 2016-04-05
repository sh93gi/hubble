package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv, OpsCenterApi}


class OpsCenterInfoSpec
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture {
  val clusterInstance: ClusterEnv = cassandragroup.last.envs.last // small cluster, such that clusterinfo is fast(er)

  describe("Opscenter info retriever"){
    it  ("should get info") {
      val metadata = new CassandraClusterApi(clusterInstance).metadata()
      val opsInfo = new OpsCenterApi(clusterInstance).getInfo(metadata)
      opsInfo should not be null
      opsInfo should not be None
      opsInfo.get.nodes.size should be > 0
    }

  }
}
