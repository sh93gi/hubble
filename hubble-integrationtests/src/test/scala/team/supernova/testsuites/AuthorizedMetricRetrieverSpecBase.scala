package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.cassandra.{CassandraClusterGroupFixture, ClusterEnv}
import team.supernova.graphite.{AuthorizedGraphiteReader, GraphiteFixture}

abstract class AuthorizedMetricRetrieverSpecBase
  extends TestKit(ActorSystem(classOf[AuthorizedMetricRetrieverSpecBase].getSimpleName))
  with FunSpecLike
  with CassandraClusterGroupFixture
  with GraphiteFixture{

  def clusterInstance: ClusterEnv = clusterEnvWithHttpAuthorizedGraphite

  /**
    * A ClusterEnv which is used to complete the graphite template url.
    * This testsuite assumes that the graphite server uses http authorization
    * Example value: cassandragroup.head.envs.last
    * @return
    */
  def clusterEnvWithHttpAuthorizedGraphite: ClusterEnv

  describe("graphite metric retrieval"){

    it  ("should retrieve measurements") {
      val metric = clusterInstance.graphiteConfig.graphite_cluster_metrics.head
      val measure = new AuthorizedGraphiteReader(metric.url_template, graphiteUserName, graphitePassword)
        .retrieve(clusterInstance.graphite)
      measure should not be None
    }

    it  ("should retrieve all metric values") {
      val measures = AuthorizedGraphiteReader.retrieveAll(
        clusterInstance.graphiteConfig.graphite_login,
        clusterInstance.graphiteConfig.graphite_cluster_metrics,
        clusterInstance.graphite)
      measures.size should be > 1
      measures.filter(_.value.isEmpty) should have size 0
    }

  }
}
