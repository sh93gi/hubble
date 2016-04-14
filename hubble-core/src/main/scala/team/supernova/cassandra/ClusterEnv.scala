package team.supernova.cassandra

import team.supernova.HubbleApp.GraphiteConfig

case class ClusterEnv( cluster_name: String,
                       graphana: String,
                       graphiteConfig: GraphiteConfig,
                       graphite: Map[String, String],
                       hosts: Array[String],
                       ops_pword: String,
                       ops_uname: String,
                       opscenter: String,
                       port: Int,
                       pword: String,
                       uname: String,
                       sequence: Int)
