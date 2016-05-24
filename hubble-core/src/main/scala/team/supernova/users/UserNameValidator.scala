package team.supernova.users

import team.supernova.validation.{Check, Severity}

import scala.collection.mutable.ListBuffer

object UserNameValidator {

  //TODO read these from properties file
  private val userNameSuffixes = Set("_owner", "_user", "_user_sm", "_user_ro", "_app_sm", "_app_ro")
  private val userNamingConventionCheckName = "User naming convention check"

  def upperCaseInNameWarning(name: String) = s"Username '$name' has upper case characters in it"

  def userNameSuffixWarning(name: String) = s"Username '$name' should end with one of the suffixes : $userNameSuffixes"

  private def hasUpperCaseLetter(name: String): Boolean = name.toLowerCase != name

  private def endsWithConvention(name: String): Boolean = userNameSuffixes.count(suffix => name.endsWith(suffix)) > 0

  private def sortChecksByDetail(checks: List[Check]): List[Check] = checks.sortBy(c => c.details)

  def namingConventionChecks(userNames: Set[String]): List[Check] = {

    def userNamingConventionWarning(warningMessage: String): Check = {
      Check(userNamingConventionCheckName, warningMessage, hasPassed = false, Severity.WARNING)
    }

    val checkResults = userNames.flatMap {
      name =>
        val checks = new ListBuffer[Check]
        //Check if there is uppercase letter in it
        if (hasUpperCaseLetter(name)) checks += userNamingConventionWarning(upperCaseInNameWarning(name))
        //Check if the user name ends with one of the legal suffixes. See userNameSuffixes
        if (!endsWithConvention(name)) checks += userNamingConventionWarning(userNameSuffixWarning(name))
        checks
    }

    sortChecksByDetail(checkResults.toList)
  }

  def keyspaceUserChecks(userNames: Set[String], keyspaceName: String): List[Check] = {

    def buildRequiredUsersListPerKeyspace(keyspaceName: String): Set[String] = {
      userNameSuffixes.map(suffix => keyspaceName + suffix)
    }

    def requiredName(requiredName: String): Check =
      Check("Missing username check", s"Username '$requiredName' is missing.", hasPassed = false, Severity.WARNING)

    val requiredUserNames = buildRequiredUsersListPerKeyspace(keyspaceName)
    requiredUserNames.map(name => if (!userNames.contains(name)) Some(requiredName(name)) else None)
      .filter(_.isDefined)
      .map(_.get)
      .toList
  }
}
