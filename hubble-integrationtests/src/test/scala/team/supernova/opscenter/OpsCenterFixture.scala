package team.supernova.opscenter

import team.supernova.cassandra.{ClusterEnv, OpsCenterApi, CassandraClusterApi}

trait OpsCenterFixture {
  def clusterInstance: ClusterEnv

  def getOpsInfo() : Option[OpsCenterClusterInfo] = {
    val metadata = new CassandraClusterApi(clusterInstance).metadata()
    val opsInfo = new OpsCenterApi(clusterInstance).getInfo(metadata)
    opsInfo
  }

}
