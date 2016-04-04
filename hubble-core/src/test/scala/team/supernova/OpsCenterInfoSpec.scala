package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}


class OpsCenterInfoSpec
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture {
  val clusterInstance: ClusterEnv = cassandragroup.head.envs.head // small cluster, such that clusterinfo is fast

  describe("Opscenter info retriever"){
    it  ("should get info") {
      val clusterInfo = new CassandraClusterApi(clusterInstance).clusterInfo("someGroup")
      val opsInfo = clusterInfo.opsCenterClusterInfo
      opsInfo should not be null
      opsInfo should not be None
      opsInfo.get.nodes.size should be > 0
    }

  }
}
