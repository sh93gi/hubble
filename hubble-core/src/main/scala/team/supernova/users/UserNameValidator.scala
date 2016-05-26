package team.supernova.users

import com.typesafe.config.ConfigFactory
import team.supernova.validation.{Check, Severity}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object UserNameValidator {

  val userNameSuffixesPath = "hubble.cassandra.username_suffixes"
  //TODO read these from properties file
  private val userNameSuffixes: Set[String] = ConfigFactory.load().getStringList(userNameSuffixesPath).asScala.toSet

  private def upperCaseInNameWarning(name: String) = s"Username '$name' has upper case characters in it"

  private def userNameSuffixWarning(name: String) = s"Username '$name' should end with one of the suffixes : $userNameSuffixes"

  private def requiredNameWarning(requiredName: String) = s"Username '$requiredName' is missing."

  private def hasUpperCaseLetter(name: String): Boolean = name.toLowerCase != name

  private def endsWithConvention(name: String): Boolean = userNameSuffixes.count(suffix => name.endsWith(suffix)) > 0

  def namingConventionChecks(userNames: Set[String]): List[Check] = {

    def userNamingConventionWarning(warningMessage: String): Check = {
      Check("User naming convention check", warningMessage, hasPassed = false, Severity.WARNING)
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

    checkResults.toList.sortBy(c => c.details)
  }

  def keyspaceUserChecks(userNames: Set[String], keyspaceName: String): List[Check] = {

    def buildRequiredUsersListPerKeyspace(keyspaceName: String): Set[String] = {
      userNameSuffixes.map(suffix => keyspaceName + suffix)
    }

    val requiredUserNames = buildRequiredUsersListPerKeyspace(keyspaceName)
    requiredUserNames.map { name =>
      if (!userNames.contains(name)) Check("Missing username check", requiredNameWarning(name), hasPassed = false, Severity.WARNING)
      else Check("Missing username check", requiredNameWarning(name), hasPassed = true, Severity.WARNING)
    }.toList
  }
}
