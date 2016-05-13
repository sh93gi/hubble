package team.supernova.graphite

import org.scalatest.FunSpecLike
import org.scalatest.Matchers._

class MetricDefinitionSpec
  extends FunSpecLike{


  describe("graphite metric definition") {

    it("should aggregate measurements") {
      val measures = List(List(1D,2D,3D))
      val transformer = MetricDefinition("burp", Some("sum"), None)
      transformer.process(MetricSource("stub"), measures).value.get should equal(6D)
    }
  }
}
