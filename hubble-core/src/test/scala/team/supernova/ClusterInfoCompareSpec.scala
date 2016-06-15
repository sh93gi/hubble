package team.supernova

import org.scalatest.{FunSpecLike, Matchers}
import team.supernova.cassandra.ClusterEnv

import scala.collection.SortedSet

class ClusterInfoCompareSpec extends FunSpecLike with Matchers {

  def clusterWithSequence(group: String, clusterName: String, sequence: Int): ClusterInfo = {
    val mockClusterEnv = getMockClusterEnv(clusterName, sequence)
    ClusterInfo(clusterName, schemaAgreement = true, Set(), List(), null, None, List(), Map(), Set(), mockClusterEnv, group)
  }

  private def getMockClusterEnv(clusterName: String, sequence: Int): ClusterEnv = {
    ClusterEnv(clusterName, "", null, Map(), Array(), "", "", "", 1, "", "", sequence, null)
  }

  describe("clusterinfo comparison") {
    it("should have 0 for equal") {
      (clusterWithSequence("group","sbx", 1) compare clusterWithSequence("group", "sbx", 1)) should be(0L)
    }

    it("should have >0 for dev compare sbx") {
      (clusterWithSequence("group","dev", 2) compare clusterWithSequence("group", "sbx", 1)) should be > 0
    }

    it("should have <0 for dev compare sbx") {
      (clusterWithSequence("group","dev", 2) compare clusterWithSequence("group", "tst", 3)) should be < 0
    }

    it("should be sorted as sbx, dev, tst") {
      val tst = clusterWithSequence("LLDS1", "tst", 3)
      val sbx = clusterWithSequence("LLDS1", "sbx", 1)
      val dev = clusterWithSequence("LLDS1", "dev", 2)

      val setOfClusters = SortedSet(tst,sbx,dev)

      setOfClusters.head.cluster_name should be(sbx.cluster_name)
      setOfClusters.tail.head.cluster_name should be(dev.cluster_name)
      setOfClusters.tail.tail.head.cluster_name should be(tst.cluster_name)

    }
  }
}
