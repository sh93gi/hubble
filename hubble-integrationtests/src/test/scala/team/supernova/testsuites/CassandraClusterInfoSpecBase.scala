package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraClusterApi, CassandraClusterGroupFixture, ClusterConnectorFixture}


abstract class CassandraClusterInfoSpecBase
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture{

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
