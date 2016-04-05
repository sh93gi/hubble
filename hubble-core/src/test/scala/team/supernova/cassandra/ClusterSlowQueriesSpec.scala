package team.supernova.cassandra

import org.scalatest.FunSpecLike
import org.scalatest.Matchers._

class ClusterSlowQueriesSpec
  extends FunSpecLike {

  describe("Cluster slow query") {

    it("should add query to cluster set") {
      val clusterSlowQueries = new ClusterSlowQueries(Some(10))
      val myQuery = new SlowQuery(List("query1"), Set("keyspace1.table1", "keyspace2.table2"), Set("keyspace1", "keyspace2"), 100)
      clusterSlowQueries.add(myQuery)
      clusterSlowQueries.clusterSlow.get(10) should contain(myQuery)
    }

    it("should add query to table set") {
      val clusterSlowQueries = new ClusterSlowQueries(Some(10))
      val myQuery = new SlowQuery(List("query1"), Set("keyspace1.table1", "keyspace2.table2"), Set("keyspace1", "keyspace2"), 100)
      clusterSlowQueries.add(myQuery)
      clusterSlowQueries.tableSlow.keySet should contain("keyspace1.table1")
      clusterSlowQueries.tableSlow.get("keyspace1.table1").get.get(10) should contain(myQuery)
      clusterSlowQueries.tableSlow.keySet should contain("keyspace2.table2")
      clusterSlowQueries.tableSlow.get("keyspace2.table2").get.get(10) should contain(myQuery)
    }

    it("should add query to keyspace set") {
      val clusterSlowQueries = new ClusterSlowQueries(Some(10))
      val myQuery = new SlowQuery(List("query1"), Set("keyspace1.table1", "keyspace2.table2"), Set("keyspace1", "keyspace2"), 100)
      clusterSlowQueries.add(myQuery)
      clusterSlowQueries.keyspaceSlow.keySet should contain("keyspace1")
      clusterSlowQueries.keyspaceSlow.get("keyspace1").get.get(10) should contain(myQuery)
      clusterSlowQueries.keyspaceSlow.keySet should contain("keyspace2")
      clusterSlowQueries.keyspaceSlow.get("keyspace2").get.get(10) should contain(myQuery)
    }
  }


}
