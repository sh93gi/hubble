package team.supernova

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, FunSpecLike}
import Matchers._
import team.supernova.cassandra.CassandraClusterApi

class CassandraSpec
  extends TestKit(ActorSystem("ClusterInfoSpec"))
  with FunSpecLike
  with CassandraTestBase {

  describe("Cassandra info retriever"){
    it  ("should connect") {
      val session = newSession()
      session should not be (null)
      session.close()
    }

    it ("should retrieve clusterinfo"){
      val clusterInfo = new CassandraClusterApi(clusterInstance).clusterInfo("someGroup")
      clusterInfo.group should be ("someGroup")
    }
  }
}
