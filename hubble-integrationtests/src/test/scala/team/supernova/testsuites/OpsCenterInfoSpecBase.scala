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

    it ("should have nonzero sstables for each node"){
      val opsInfo = getOpsInfo()
      opsInfo.get.nodes.size should be > 0
      all (opsInfo.get.nodes.map( // For each node sum of SSTables (over all keyspaces, over all tables)
        _.opsKeyspaceInfoList.flatMap(_.opsTableInfoList.map(_.numberSSTables)).sum
      )) should be >=(0L)
    }

  }
}
