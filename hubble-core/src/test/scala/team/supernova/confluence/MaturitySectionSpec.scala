package team.supernova.confluence

import org.scalatest.{FunSpecLike, Matchers}
import team.supernova.results.MaturityAspect
import team.supernova.validation.{Check, Severity}

class MaturitySectionSpec
    extends FunSpecLike with Matchers {
  val stubAspectFailed = MaturityAspect(0.4, Check("name", "details", hasPassed = false, Severity.WARNING))
  val stubAspectSuccess = MaturityAspect(0.4, Check("name", "details", hasPassed = true, Severity.WARNING))

  describe("MaturitySection") {
    describe("present level") {
      it("should handle empty checks") {
        MaturitySection.presentLevel(1, List())
      }

      it("should handle checks") {
        MaturitySection.presentLevel(1, List(stubAspectFailed))
      }
    }

    describe("present maturity") {
      it("should handle empty map") {
        MaturitySection.present("header", Map())
      }
      it("should handle with map") {
        MaturitySection.present("header", Map(1 -> List(stubAspectFailed)))
      }
      it("should contain header") {
        MaturitySection.present("header", Map(1 -> List(stubAspectFailed))).toString() should include("<h2>header</h2>")
        MaturitySection.present("header", Map(1 -> List(stubAspectFailed))).toString() should include("<h3>Maturity level 1 : 0")
        MaturitySection.present("header", Map(1 -> List(stubAspectFailed))).toString() should include("00%</h3>")
        MaturitySection.present("header", Map(1 -> List(stubAspectSuccess))).toString() should include("<h3>Maturity level 1 : 100")
        MaturitySection.present("header", Map(1 -> List(stubAspectSuccess))).toString() should include("00%</h3>")
      }
    }
  }
}
