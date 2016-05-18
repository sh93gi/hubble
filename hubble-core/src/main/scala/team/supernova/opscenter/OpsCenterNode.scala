package team.supernova.opscenter

case class OpsCenterNode(name: String, cassandra: Option[CassandraYaml], opsKeyspaceInfoList: List[OpsKeyspaceInfo])
