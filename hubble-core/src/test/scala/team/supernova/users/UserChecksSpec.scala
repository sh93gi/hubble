package team.supernova.users

import org.scalatest.{FunSpecLike, Matchers}

class UserChecksSpec extends FunSpecLike with Matchers {

  describe("User naming checks") {
    it("should fail for both upper case chars and suffix checks") {
      val userNames = Set("InvalidName")

      val checks = UserNameValidator.namingConventionChecks(userNames)

      checks.size should be(2)
    }

    it("should fail for both upper case chars and suffix checks 2") {
      val userNames = Set("invalidName")

      val checks = UserNameValidator.namingConventionChecks(userNames)

      checks.size should be(2)
    }

    it("should fail for suffix checks") {
      val userNames = Set("invalid_name")

      val checks = UserNameValidator.namingConventionChecks(userNames)

      checks.size should be(1)
    }

    it("should be successful") {
      val userNames = Set("valid_name_owner")

      val checks = UserNameValidator.namingConventionChecks(userNames)

      checks.size should be(0)
    }

    it("should be successful 2") {
      val userNames = Set("validname_owner")

      val checks = UserNameValidator.namingConventionChecks(userNames)

      checks.size should be(0)
    }

    it("Should be successful 3") {
      val userNames = Set("ideal_owner","ideal_app_ro","ideal_app_sm","ideal_user","ideal_user_ro","ideal_user_sm")

      val checks = UserNameValidator.keyspaceUserChecks(userNames,"ideal")

      checks.size should be(0)
    }

    it("Should be missing ideal_owner") {
      val userNames = Set("ideal_app_ro","ideal_app_sm","ideal_user","ideal_user_ro","ideal_user_sm")

      val checks = UserNameValidator.keyspaceUserChecks(userNames,"ideal")

      checks.size should be(1)

      checks.head.details.contains("ideal_owner") should be(true)

    }
  }

}
