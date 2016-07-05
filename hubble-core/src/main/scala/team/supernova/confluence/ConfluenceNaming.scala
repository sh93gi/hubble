package team.supernova.confluence

import team.supernova.confluence.soap.rpc.soap.beans.RemotePageSummary
import team.supernova.{ClusterInfo, Keyspace}

object ConfluenceNaming {
  val deletionSuffix = "[deleted]"

  def createDeletedName(title: String): String = {
     title + deletionSuffix
  }

  def hasDeletedName(title: String) : Boolean = {
    title.endsWith(deletionSuffix)
  }

  def createLink(project: String, clusterName: String, keyspaceName: String) = {
    s"/display/$project/${createName(clusterName, keyspaceName).replace(" ", "+")}"
  }

  def createLink(project: String, clusterInfo: ClusterInfo, keyspace: Keyspace) = {
    s"/display/$project/${createName(clusterInfo, keyspace).replace(" ","+")}"
  }

  def createMetricsLink(project: String, cluster:ClusterInfo): String = {
    s"/display/$project/${createMetricsName(cluster).replace(" ","+")}"
  }

  def createMetricsLink(project: String, cluster:String): String = {
    s"/display/$project/${createMetricsName(cluster).replace(" ","+")}"
  }


  def createMetricsName(cluster: ClusterInfo): String = {
    createName(cluster.cluster_name) + "_metrics"
  }

  def createMetricsName(cluster_name: String): String = {
    createName(cluster_name) + "_metrics"
  }

  def createName(cluster: ClusterInfo): String = {
    createName(cluster.cluster_name)
  }

  def createName(cluster_name: String): String = {
    cluster_name.toUpperCase
  }

  def createName(cluster: ClusterInfo, keyspace: Keyspace): String = {
    cluster.cluster_name.toUpperCase + " - " + keyspace.keyspace_name.toUpperCase
  }

  def createName(cluster: String, keyspace: String): String = {
    cluster.toUpperCase + " - " + keyspace.toUpperCase
  }

  def createMetricsLink(project: String, clusterInfo: ClusterInfo, keyspace: Keyspace): String = {
    createMetricsLink(project, clusterInfo.cluster_name, keyspace.keyspace_name)
  }

  def createMetricsLink(project: String, clusterInfo: String, keyspace: String): String = {
    s"/display/$project/${createMetricsName(clusterInfo, keyspace).replace(" ","+")}"
  }

  def createMetricsName(clusterInfo: ClusterInfo, keyspace: Keyspace): String = {
    createName(clusterInfo, keyspace) + " - metrics"
  }

  def createMetricsName(clusterInfo: String, keyspace: String): String = {
    createName(clusterInfo, keyspace) + " - metrics"
  }

  def hasNameOf(cluster: ClusterInfo, page: RemotePageSummary): Boolean = {
    createName(cluster) equals page.getTitle
  }

  def substringAfter(s: String, k: String) = {
    s.indexOf(k) match {
      case -1 => "";
      case i => s.substring(i + k.length)
    }
  }

  def hasNameOf(keyspace: Keyspace, page: RemotePageSummary): Boolean = {
    keyspace.keyspace_name.toUpperCase.equals(substringAfter(page.getTitle, " - "))
  }

  def hasMetricsNameOf(cluster: ClusterInfo, page: RemotePageSummary): Boolean = {
    createName(cluster) equals page.getTitle
  }

}

