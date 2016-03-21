package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.Matchers._
import org.scalatest.{FunSpecLike, Matchers}
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
      println(opsInfo)
      val rows = opsInfo.get.nodes.
        flatMap(n => n.opsKeyspaceInfoList.flatMap(k=> k.opsTableInfoList.map(t=> Tuple5(k.keyspaceName, t.tableName, n.name, t.avgDataSizeMB, t.numberSSTables ))))
      println(rows)
    }

  }
}
