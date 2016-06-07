package team.supernova.users

import team.supernova.validation.{Check, Severity}

import scala.util.matching.Regex

object UserNameValidator{
  def apply(nameRegex: Regex, allowed_suffixes: Set[String]): UserNameValidator = {
    new UserNameValidator(nameRegex, allowed_suffixes)
  }
}

class UserNameValidator(userNameRegex: Regex, userNameSuffixes: Set[String]) {


  private val userNameSuffixesString = userNameSuffixes.mkString(", ") // we do not want to build this string for each warning message

  private def userNameSuffixWarning(name: String): String = s"Username '$name' should end with one of the suffixes : $userNameSuffixesString"

  private def upperCaseInNameWarning(name: String): String = s"Username '$name' has upper case characters in it"

  private def requiredNameWarning(requiredName: String): String = s"Username '$requiredName' is missing."

  private val namingConventionCheckName: String = "User naming convention check"

  def namingConventionChecks(userNames: Set[String]): List[Check] = userNames.flatMap { name =>
    List[Check](
      Check(namingConventionCheckName, upperCaseInNameWarning(name), matchesRegex(name), Severity.WARNING),
      Check(namingConventionCheckName, userNameSuffixWarning(name), endsWithConvention(name), Severity.WARNING)
    )
  }.toList.sortBy(c => c.details)

  private def matchesRegex(name: String): Boolean = userNameRegex findFirstIn name isDefined

  private def endsWithConvention(name: String): Boolean = userNameSuffixes.exists(name.endsWith)

  private val missingUserNameCheckName: String = "Missing username check"

  def keyspaceUserChecks(userNames: Set[String], keyspaceName: String): List[Check] = userNameSuffixes.map { suffix =>
      val requiredName = keyspaceName + suffix
      Check(missingUserNameCheckName, requiredNameWarning(requiredName), userNames.contains(requiredName), Severity.WARNING)
    }.toList

}
