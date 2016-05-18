package team.supernova.graphite

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.Matchers._
import org.scalatest.{FunSpecLike, Matchers}
import team.supernova.CassandraClusterGroupFixture

class MetricDefinitionSpec
  extends TestKit(ActorSystem(classOf[MetricDefinitionSpec].getSimpleName))
  with FunSpecLike
  with CassandraClusterGroupFixture {


  describe("graphite metric definition") {

    it("should aggregate measurements") {
      val measures = List(List(1D,2D,3D))
      val transformer = MetricDefinition("burp", Some("sum"), None)
      transformer.process(MetricSource("stub"), measures).value.get should equal(6D)
    }
  }
}
