package team.supernova.opscenter

import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}
import scalaj.http._

object OpsCenter {
  val readTimeout = 20000
  val connTimeout = 10000

  val log = LoggerFactory.getLogger(OpsCenter.getClass)

  implicit def HttpSessionToHttpRequest(httpSession: HttpSession) : HttpRequest = httpSession.http

  implicit class HttpSession(val http: HttpRequest){
    def withHeaders(login: Login): HttpSession = {
      http.header("opscenter-session", login.sessionid)
        .header("Accept", "text/json")
    }
  }

  def tryNodeNames(host: String, login: Login, clusterName: String): Try[List[String]] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    val nodesAttempt = Try(Http(s"$host/$clusterName/nodes")
        .withHeaders(login)
        .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
        .asString.body)
        .logFailure(e => log.error(s"Failed to retrieve nodenames content of $clusterName from $host, because of ${e.getMessage} (${e.getClass.getName})"))
        .logSuccess(r => log.info(s"Successfully retrieved nodenames content of $clusterName from $host"))
    nodesAttempt.flatMap(response => Try(
      (parse(response) \\ "node_ip").children.map(_.values.toString))
        .logFailure(e => log.error(s"Failed to parse the retrieved nodenames content of $clusterName from $host, because of ${e.getMessage} (${e.getClass.getName}), orig content=$response"))
        .logSuccess(e => {
          if (e.isEmpty)
            log.error(s"Failed to find any nodes in the nodes description of $clusterName from $host. The response was: '$response'")
          else
            log.info(s"Successfully parsed the retrieved nodenames content of $clusterName from $host. Found ${e.size} nodes.")
        })
    )
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
            .map(v => Math.round(v / 1048576)).getOrElse(-1),
          tryMetric(login, host, uname, pword, clusterName, node, keyspace_tables._1, tableName, "cf-live-sstables")
            .getOrElse(-1D).toLong
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
    val loginAttempt = Try(Http(s"$host/login")
      .param("username", uname).param("password", pword)
      .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
      .asString.body)
    loginAttempt.flatMap(response => Try(Login.parseLogin(response)))
  }

  def tryYaml(login: Login, host: String, clusterName: String, node_ip: String): Try[CassandraYaml] = {
    val nodeconfAttempt = Try(Http(s"$host/$clusterName/nodeconf/$node_ip")
        .withHeaders(login)
        .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
        .asString.body)
      .logFailure(e => log.error(s"Failed to retrieve nodeconf content of $clusterName from $host, because of ${e.getMessage} (${e.getClass.getName})"))
      .logSuccess(r => log.info(s"Successfully retrieved nodeconf content of $clusterName from $host"))
    nodeconfAttempt.flatMap(response =>
      Try(CassandraYaml.parseBody(response))
      .logFailure(e => log.error(s"Failed to parse the retrieved nodeconf content of $clusterName from $host, because of ${e.getMessage} (${e.getClass.getName}), orig content=$response"))
      .logSuccess(r => {
          log.info(s"Successfully parsed the retrieved nodeconf content of $clusterName from $host. Found ${r.all.size} key value pairs.")
      })
    )
  }

  def tryMetric(login: Login,
                host: String,
                uname: String,
                pword: String,
                clusterName: String,
                node: String,
                keyspaceName: String,
                tableName: String,
                metricName: String): Option[Double] = {
    val url = s"$host/$clusterName/metrics/$node/$keyspaceName/$tableName/$metricName?step=120&start=${System.currentTimeMillis / 1000 - 300}&function=max"
    val metricattempt = Try(Http(url)
        .withHeaders(login)
        .timeout(connTimeoutMs = connTimeout, readTimeoutMs = readTimeout)
        .asString.body)
    metricattempt
      .logFailure(e => log.error(s"Failed to receive $metricName data of $keyspaceName on $node in $clusterName, using $url, because of ${e.getMessage}"))
      .logSuccess(metric => log.info(s"Received $metricName raw data of $keyspaceName on $node in $clusterName. Value = $metric"))
      .flatMap(response => Try(parseMetric(response))
      .logFailure(e => s"Failed to parse $metricName data of $keyspaceName on $node in $clusterName, because of ${e.getMessage}, metric data=$response"))
      .logSuccess(metric => log.info(s"Interpreted $metricName value of $keyspaceName on $node in $clusterName. Value = $metric"))
      .toOption.flatten
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
      .logFailure(e => log.error(s"Failed to login on OpsCenter on $host for $clusterName: ${e.getMessage} ${e.getClass.getCanonicalName}"))
      .logSuccess(_ => log.info(s"Successfully logged on OpsCenter on $host for $clusterName"))
      .flatMap(login => {
        tryNodeNames(host, login, clusterName).map(res=>(login, res))
      })
      .logSuccess(res => log.debug(s"Successfully received node names from OpsCenter within $clusterName : ${res._2.mkString(", ")}"))
      .logFailure(e => log.error(s"Failed to retrieve node names from OpsCenter within $clusterName : ${e.getMessage} ${e.getClass.getCanonicalName}"))
      .toOption
      .flatMap(login_nodes=>{
        val login = login_nodes._1
        val listNodeIP = login_nodes._2
        //per node
        val listNodes = listNodeIP.map(node_ip => {
          val nodeYaml = tryYaml(login, host, clusterName, node_ip)
            .logSuccess(res => log.debug(s"Successfully loaded node yaml configuration of $node_ip within $clusterName , containing the following keys: ${res.all.keys.mkString(", ")} "))
            .logFailure(e => log.error(s"Failed to load node yaml configuration of $node_ip within $clusterName: ${e.getMessage} ${e.getClass.getCanonicalName}"))
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
