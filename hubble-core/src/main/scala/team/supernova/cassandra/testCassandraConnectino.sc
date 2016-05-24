import team.supernova.users.UserNameValidator
import team.supernova.validation.Check

val users = Set("InvalidName","Invalid_Name", "invalidName", "invalid_name", "valid_name_user_ro", "valid_owner", "authentication_api_app_ro")

val keyspaceNames = Set("ideal", "zkv", "authentication_api")

val userNamingChecks : List[Check] = UserNameValidator.namingConventionChecks(users)


userNamingChecks.foreach(println)

val keyspaceUserChecks = UserNameValidator.keyspaceUserChecks(users,keyspaceNames)

keyspaceUserChecks.foreach(println)
