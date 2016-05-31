package team.supernova.validation

case class Check(name: String, details: String, hasPassed: Boolean, severity: String, suggestion : Option[String] = None)
