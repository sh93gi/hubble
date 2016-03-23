package team.supernova.actor.collect

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import team.supernova.cassandra.ClusterEnv

trait ClusterWorkerBatch[T] extends Actor with ActorLogging {
  /**
    * Counter of number of clusters that are in progress
    */
  var counter = 0
  /**
    * All received results so far
    */
  var clusterResults: Set[T] = Set.empty

  /**
    * The batch of workers, of which each one will work on a single cluster(env)
    */
  val clusterWorkers = context.actorOf(RoundRobinPool(5).props(newActor()), "clusterWorker")

  /**
    * Start analyzing all cluster groups.
    * @param clusterGroups the collection of clustergrougs (each one holding a set of clusters)
    */
  def runOnAll(clusterGroups: List[CassandraClusterGroup]) = {
    clusterGroups.foreach(cluster => {
      log.info(s"Get all clusterInfo for - ${cluster.name}")
      cluster.envs.foreach(row => processClusterInfoPerRow(row, cluster.name))
    })
  }

  /**
    * Start analyzing a single cluster(env)
    * @param clusterEnv the cluster to be analyzed
    * @param clusterGroup the (logical) group the cluster belongs to, sometimes usefull for the worker
    */
  def processClusterInfoPerRow(clusterEnv: ClusterEnv, clusterGroup: String) = {
    // ask for new cluster info
    counter += 1
    log.info(s"Querying cassandra - " + clusterEnv.cluster_name)
    clusterWorkers ! message(clusterEnv, clusterGroup)
  }

  /**
    * To be called by the subclass when a result has been received.
    * Will keep track of number of outstanding requests, and will call finished if all work has been done.
    * @param clusterResult the single result element received from one of the workers
    */
  def received(clusterResult: T): Unit = {
    counter -= 1
    clusterResults += clusterResult
    if (counter == 0)
      finished(clusterResults)
  }

  /**
    * Factory method for a new akka actor, of which a set of instances will be created to distibute the work amongs
    * @return akka props allowing creation of akka instances.
    *         The created instances will receive messages to work on single cluster(env)s
    */
  def newActor(): Props

  /**
    * Method that will be called when all results have been gathered
    *
    * @param clusterBatchResults the collected results
    */
  def finished(clusterBatchResults: Set[T]) : Unit

  /**
    * Factory method to create the akka message expected by your newActor()
    * @param clusterEnv the single cluster(env) to be analyzed
    * @param clusterGroup the group name it belongs to
    * @return an akka message, which will be sent to one of the intances createed by newActor()
    */
  def message(clusterEnv: ClusterEnv, clusterGroup: String) : Any
}




