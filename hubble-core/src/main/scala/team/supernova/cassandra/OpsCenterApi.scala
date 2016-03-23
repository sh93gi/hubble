package team.supernova.cassandra

import com.datastax.driver.core.Metadata
import team.supernova.{Keyspace, OpsCenter, OpsCenterClusterInfo}

import scala.collection.JavaConversions._
import scala.collection.SortedSet

class OpsCenterApi(cluster: ClusterEnv) {

  def getInfo(metaData: Metadata) : Option[OpsCenterClusterInfo] ={
    val dataCenter: SortedSet[String] = metaData.getAllHosts.groupBy(h => h.getDatacenter).keys.to
    val keyspaces: SortedSet[Keyspace] = metaData.getKeyspaces.map(i => {
      new Keyspace(i, dataCenter)
    }).to
    val opsKeyInfo: Map[String, List[String]]  = keyspaces.foldLeft(Map[String, List[String]]()){ (a,b) => a ++ Map( b.keyspace_name -> b.tables.map(_.table_name) ) }
    OpsCenter.createOpsCenterClusterInfo(cluster.opscenter, cluster.ops_uname, cluster.ops_pword, metaData.getClusterName(), opsKeyInfo )
    }
}
