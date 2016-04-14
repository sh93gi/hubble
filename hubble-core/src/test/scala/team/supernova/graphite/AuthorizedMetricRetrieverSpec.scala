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

    it  ("should retrieve metric value") {
      val metric = clusterInstance.graphiteConfig.graphite_metrics.head
      val measure = new AuthorizedMetricRetriever(metric.url_template, metric.func, graphiteUserName, graphitePassword)
        .retrieve(clusterInstance.graphite)
      measure should not be None
      measure.get should be > 0D
    }

    it  ("should retrieve all metric values") {
      val measures = AuthorizedMetricRetriever.retrieveAll(clusterInstance)
      measures.size should be > 1
      measures.filter(_._2.isEmpty) should have size 0
    }

  }
}
