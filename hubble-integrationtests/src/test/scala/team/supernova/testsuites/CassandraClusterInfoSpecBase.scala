package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{ClusterEnv, CassandraClusterApi, CassandraClusterGroupFixture, ClusterConnectorFixture}


abstract class CassandraClusterInfoSpecBase
  extends TestKit(ActorSystem("CassandraClusterInfoSpec"))
  with FunSpecLike
  with ClusterConnectorFixture
  with CassandraClusterGroupFixture{

  /**
    * A cluster instance from which the metadata will be retrieved. Smaller ones are faster.
    * Example value: cassandragroup.head.envs.last
    * @return
    */
  override def clusterInstance: ClusterEnv

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
