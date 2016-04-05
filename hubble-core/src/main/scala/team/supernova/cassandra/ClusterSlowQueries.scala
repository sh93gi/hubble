package team.supernova.cassandra

import com.datastax.driver.core.exceptions.UnauthorizedException

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ClusterSlowQueries(limit: Option[Int]){
  def failed(e: UnauthorizedException) : Unit = {
    unauthorized += e
  }

  def failed(e: Throwable): Unit = {
    failed += e
  }

  val unauthorized = new ArrayBuffer[UnauthorizedException]()
  val failed = new ArrayBuffer[Throwable]()
  val clusterSlow = new TopSlowestQueries(limit)
  val tableSlow = mutable.Map[String, TopSlowestQueries]()
  val keyspaceSlow = mutable.Map[String, TopSlowestQueries]()

  def add(queryDetails: SlowQuery){
    clusterSlow.add(queryDetails)
    queryDetails.keyspaces.foreach(keyspaceSlow.getOrElseUpdate(_, new TopSlowestQueries(limit)).add(queryDetails))
    queryDetails.tables.foreach(tableSlow.getOrElseUpdate(_, new TopSlowestQueries(limit)).add(queryDetails))
  }
}

