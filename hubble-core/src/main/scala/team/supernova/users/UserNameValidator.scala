package team.supernova.users

import com.typesafe.config.{Config, ConfigFactory}
import team.supernova.validation.{Check, Severity}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

object UserNameValidator {

  // Config read
  private val config: Config = ConfigFactory.load()
  private val userNameSuffixesPath: String = "hubble.cassandra.username_suffixes"
  private val userNameRegexPath: String = "hubble.cassandra.username_regex"
  private val userNameSuffixes: Set[String] = config.getStringList(userNameSuffixesPath).asScala.toSet
  private val userNameRegex: Regex = config.getString(userNameRegexPath).r

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
