package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra._
import team.supernova.opscenter.OpsCenterFixture


abstract class OpsCenterInfoSpecBase
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with OpsCenterFixture
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture {

  val clusterInstance: ClusterEnv = clusterEnvWithOpsCenter

  /**
    * A clusterEnv which has an OpsCenter defined
    * example value: cassandragroup.last.envs.last
    * @return
    */
  def clusterEnvWithOpsCenter : ClusterEnv

  describe("Opscenter info retriever"){
    it  ("should get info") {
      val opsInfo = getOpsInfo()
      opsInfo should not be null
      opsInfo should not be None
      opsInfo.get.nodes.size should be > 0
    }

    it ("should have nonzero sstables"){
      val opsInfo = getOpsInfo()
      opsInfo.get.nodes.size should be > 0
      opsInfo.get.nodes.head.opsKeyspaceInfoList.head.opsTableInfoList.head.numberSSTables should be > 0L
    }

  }
}
