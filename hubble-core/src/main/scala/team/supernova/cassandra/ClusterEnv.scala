package team.supernova.cassandra

import team.supernova.graphite.GraphiteConfig
import team.supernova.users.UserNameValidator

case class ClusterEnv( cluster_name: String,
                       graphana: String,
                       graphiteConfig: GraphiteConfig,
                       graphite: List[Map[String, String]],
                       hosts: Array[String],
                       ops_pword: String,
                       ops_uname: String,
                       opscenter: String,
                       port: Int,
                       pword: String,
                       uname: String,
                       sequence: Int,
                       usernameValidator: UserNameValidator)
