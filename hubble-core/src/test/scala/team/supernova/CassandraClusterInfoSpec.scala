package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, FunSpecLike}
import Matchers._
import team.supernova.cassandra.{ClusterEnv, CassandraClusterApi}


class CassandraClusterInfoSpec
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture{
  val clusterInstance: ClusterEnv = cassandragroup.head.envs.last // small cluster, such that clusterinfo is fast

  describe("Cassandra info retriever"){
    it  ("should connect") {
      val session = newSession()
      session should not be null
      session.close()
    }

    it ("should retrieve clusterinfo"){
      val clusterInfo = new CassandraClusterApi(clusterInstance).clusterInfo("someGroup")
      clusterInfo.group should be ("someGroup")
    }

  }
}
