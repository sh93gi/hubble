package team.supernova.opscenter

case class OpsCenterClusterInfo(login: Login,
                                name: String,
                                nodes: List[OpsCenterNode]
                               )
