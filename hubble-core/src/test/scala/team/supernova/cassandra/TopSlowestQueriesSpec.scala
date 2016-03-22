package team.supernova.cassandra

import org.scalatest.FunSpecLike
import org.scalatest.Matchers._

class TopSlowestQueriesSpec
 extends FunSpecLike {

  describe("Top slowest query selector"){

    it("should select replace same query with slowest version"){
      val fast = new SlowQuery(List("test1"), Set(), Set(), 100)
      val slow = new SlowQuery(List("test1"), Set(), Set(), 200)
      val selector = new TopSlowestQueries(None)
      selector.add(fast)
      selector.add(slow)
      selector.get(2) should have size 1
      selector.get(2).head should equal(slow)
    }

    it("should keep slowest queries"){
      val some1 = (1 to 30).map(i => new SlowQuery(List("test"+i), Set(), Set(), i*100))
      val selector = new TopSlowestQueries(Some(10))
      some1.foreach(selector.add)
      selector.get() should have size 10
      selector.get(1).head should equal(some1.last)
    }

    it("should internally keep at most twice the number of queries"){
      val some1 = (1 to 30).map(i => new SlowQuery(List("test"+i), Set(), Set(), i*100))
      val selector = new TopSlowestQueries(Some(10))
      some1.foreach(selector.add)
      selector.commands.size should be <= 20
    }
  }

}
