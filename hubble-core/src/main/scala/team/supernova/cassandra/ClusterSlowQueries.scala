package team.supernova.cassandra

import scala.collection.mutable

class ClusterSlowQueries(limit: Option[Int]){

  val clusterSlow = new TopSlowestQueries(limit)
  val tableSlow = mutable.Map[String, TopSlowestQueries]()
  val keyspaceSlow = mutable.Map[String, TopSlowestQueries]()

  def add(queryDetails: SlowQuery){
    clusterSlow.add(queryDetails)
    queryDetails.keyspaces.foreach(keyspaceSlow.getOrElseUpdate(_, new TopSlowestQueries(limit)).add(queryDetails))
    queryDetails.tables.foreach(tableSlow.getOrElseUpdate(_, new TopSlowestQueries(limit)).add(queryDetails))
  }
}

