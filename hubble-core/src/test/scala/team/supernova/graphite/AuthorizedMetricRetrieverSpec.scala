package team.supernova.graphite

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.CassandraClusterGroupFixture
import team.supernova.cassandra.ClusterEnv

class AuthorizedMetricRetrieverSpec
  extends TestKit(ActorSystem(classOf[AuthorizedMetricRetrieverSpec].getSimpleName))
    with FunSpecLike
    with CassandraClusterGroupFixture
with GraphiteFixture{
  val clusterInstance: ClusterEnv = cassandragroup.head.envs.last

  describe("graphite metric retrieval"){

    it  ("should retrieve measurements") {
      val metric = clusterInstance.graphiteConfig.graphite_metrics.head
      val measure = new AuthorizedGraphiteReader(metric.url_template, graphiteUserName, graphitePassword)
        .retrieve(clusterInstance.graphite)
      measure should not be None
    }

    it  ("should retrieve all metric values") {
      val measures = AuthorizedGraphiteReader.retrieveAll(clusterInstance)
      measures.size should be > 1
      measures.filter(_.value.isEmpty) should have size 0
    }

  }
}
