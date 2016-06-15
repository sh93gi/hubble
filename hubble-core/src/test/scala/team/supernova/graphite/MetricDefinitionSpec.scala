package team.supernova.graphite

import org.scalatest.FunSpecLike
import org.scalatest.Matchers._
import team.supernova.validation.Check

class MetricDefinitionSpec
  extends FunSpecLike{


  describe("graphite metric definition") {

    it("should aggregate measurements") {
      val measures = List(List(1D,2D,3D))
      val transformer = MetricDefinition(new GraphiteMetricConfig("url", "name", Some("sum"), None, List()))
      transformer.process(MetricSource("stub"), measures, Map()).value.get should equal(6D)
    }

    def verifyMetricCheck(value: Double, check: String, threshold: Double, hasPassed: Boolean) = {
      val measures = List(List(value))
      val transformer = MetricDefinition(new GraphiteMetricConfig("url", "name", Some("sum"), None, List(
        new GraphiteMetricCheckConfig("name", "details", check, threshold, "myseverity"))))
      val result = transformer.process(MetricSource("stub"), measures, Map())
      result.checks.head should equal(Check("name", "details", hasPassed, "myseverity"))
    }

    it("should succeed on less than check") {
      verifyMetricCheck(6, "lt", 7, hasPassed = true)
    }

    it("should fail on less than check") {
      verifyMetricCheck(6, "lt", 5, hasPassed = false)
    }

    it("should fail on equality check") {
      verifyMetricCheck(6, "eq", 5, hasPassed = false)
    }

    it("should succeed on equality check") {
      verifyMetricCheck(6, "eq", 6, hasPassed = true)
    }

    it("should succeed on greater than check") {
      verifyMetricCheck(6, "gt", 5, hasPassed = true)
    }

    it("should fail on greater than check") {
      verifyMetricCheck(6, "gt", 7, hasPassed = false)
    }
  }
}
