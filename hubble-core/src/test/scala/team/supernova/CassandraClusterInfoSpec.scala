package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.Matchers._
import org.scalatest.{FunSpecLike, Matchers}
import team.supernova.cassandra.{CassandraClusterApi, ClusterEnv}


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
      val metadata = new CassandraClusterApi(clusterInstance).metadata()
      metadata.getClusterName  should be (clusterInstance.cluster_name)
    }

  }
}
