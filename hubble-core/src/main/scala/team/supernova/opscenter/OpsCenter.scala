package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}
import scalaj.http._

object OpsCenter {
  val readTimeout = 20000
  val connTimeout = 10000

  val log = LoggerFactory.getLogger(OpsCenter.getClass)

  def tryNodeNames(host: String, login: Login, clusterName: String): Try[List[String]] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    val nodesAttempt = Try(Http(s"http://$host/$clusterName/nodes").header("opscenter-session", login.sessionid)
      .header("Accept", "text/json")
      .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
      .asString.body)
    nodesAttempt.flatMap(response => Try((parse(response) \\ "node_ip").children.map(_.values.toString)))
  }


  def getTableSize(login: Login,
                   host: String,
                   uname: String,
                   pword: String,
                   clusterName: String,
                   listKeyspaceInfo: Map[String, List[String]],
                   node: String
                  ): List[OpsKeyspaceInfo] = {
    listKeyspaceInfo.map(keyspace_tables=>
      (keyspace_tables._1, keyspace_tables._2.map(tableName=>
        OpsTableInfo(tableName,
          tryMetric(login, host, uname, pword, clusterName, node, keyspace_tables._1, tableName, "cf-total-disk-used")
            .toOption.flatten.map(v => Math.round(v / 1048576)).getOrElse(-1),
          tryMetric(login, host, uname, pword, clusterName, node, keyspace_tables._1, tableName, "cf-live-sstables")
            .toOption.flatten.getOrElse(-1D).toLong
        )
      ))
    ).toList.map(k_v=>OpsKeyspaceInfo(k_v._1, k_v._2))
  }

  def parseMetric(response: String): Option[Double] = {
    val retVal: Option[Double] = (parse(response) \\ "MAX").children.map(_.values).headOption.flatMap(_.asInstanceOf[List[Double]].lastOption)
    retVal
  }

  def tryLogin(host: String, uname: String, pword: String): Try[Login] = {
    //login to OpsCenter and get session id
    val loginattempt = Try(Http(s"http://$host/login").param("username", uname).param("password", pword)
      .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
      .asString.body)
    loginattempt.flatMap(response => Try(Login.parseLogin(response)))
  }

  def tryYaml(login: Login, host: String, clusterName: String, node_ip: String): Try[CassandraYaml] = {
    val nodeconfattempt = Try(Http(s"http://$host/$clusterName/nodeconf/$node_ip")
      .header("opscenter-session", login.sessionid)
      .header("Accept", "text/json")
      .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
      .asString.body)
    nodeconfattempt.flatMap(response => Try(CassandraYaml.parseBody(response)))
  }

  def tryMetric(login: Login,
                host: String,
                uname: String,
                pword: String,
                clusterName: String,
                node: String,
                keyspaceName: String,
                tableName: String,
                metricName: String): Try[Option[Double]] = {
    val url = s"http://$host/$clusterName/metrics/$node/${keyspaceName}/$tableName/$metricName?step=120&start=${System.currentTimeMillis / 1000 - 300}&function=max"
    val metricattempt = Try(Http(url)
      .header("opscenter-session", login.sessionid)
      .header("Accept", "text/json")
      .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
      .asString.body)
    metricattempt
      .logFailure(e => log.error(s"Failed to receive $metricName data of $keyspaceName on $node in $clusterName, using $url, because of ${e.getMessage}"))
      .logSuccess(metric => log.info(s"Received $metricName raw data of $keyspaceName on $node in $clusterName. Value = $metric"))
      .flatMap(response => Try(parseMetric(response))
        .logFailure(e => s"Failed to parse $metricName data of $keyspaceName on $node in $clusterName, because of ${e.getMessage}, metric data=$response"))
      .logSuccess(metric => log.info(s"Interpreted $metricName value of $keyspaceName on $node in $clusterName. Value = $metric"))
  }

  implicit class LogTry[A](attempt: Try[A]) {

    def logSuccess(logger: A => Unit): Try[A] = {
      attempt.map(res=>{
        logger(res)
        res
      })
    }

    def logFailure(logger: Throwable => Unit): Try[A] = {
      attempt recoverWith {
        case e: Throwable =>
          logger(e)
          new Failure(e)
      }
    }
  }

  //TODO check for more ideas - http://docs.datastax.com/en/opscenter/5.1/api/docs/index.html#
  def gatherOpsCenterClusterInfo(host: String,
                                 uname: String,
                                 pword: String,
                                 clusterName: String,
                                 listKeyspaceInfo: Map[String, List[String]]
                                ): Option[OpsCenterClusterInfo] = {
    tryLogin(host, uname, pword)
      .logFailure(e => log.error("Failed to login on $host for $clusterName: ${e.getMessage()}"))
      .logSuccess(_ => log.info(s"Successfully logged in to opscenter for $clusterName"))
      .flatMap(login => {
        tryNodeNames(host, login, clusterName).map(res=>(login, res))
      })
      .logSuccess(res => log.debug(s"Found the following nodes in OpsCenter: ${res._2}"))
      .toOption
      .flatMap(login_nodes=>{
        val login = login_nodes._1
        val listNodeIP = login_nodes._2
        //per node
        val listNodes = listNodeIP.map(node_ip => {
          val nodeYaml = tryYaml(login, host, clusterName, node_ip)
            .logFailure(e => log.error(s"Failed to load node configuration of $node_ip within $clusterName"))
            .toOption
          val keyInfo = getTableSize(login, host, uname, pword, clusterName, listKeyspaceInfo, node_ip)
          new OpsCenterNode(node_ip, nodeYaml, keyInfo)
        })
        Some(new OpsCenterClusterInfo(login, clusterName, listNodes))
      })
  }


  //TODO - ClusterName now input!  - This code can be used to find unknown cluster :-)
  //    //find list of clusters
  //    //TODO - val clusterCOnfig = ClusterConfig.parseClusterConfig(clusterConfigRespone)
  //    val listClusterName= getClusterNames (host, login)
  //    println (listClusterName)
}
