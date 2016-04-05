package team.supernova.cassandra

case class ClusterEnv( cluster_name: String,
                       graphana: String,
                       graphite_template: String,
                       graphite: Map[String, String],
                       hosts: Array[String],
                       ops_pword: String,
                       ops_uname: String,
                       opscenter: String,
                       port: Int,
                       pword: String,
                       uname: String,
                       sequence: Int)
