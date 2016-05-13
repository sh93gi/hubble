package team.supernova.testsuites

import akka.actor.ActorSystem
import akka.testkit._
import org.scalatest._
import team.supernova.confluence.soap.rpc.soap.actions.{Page, Token}
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage
import team.supernova.confluence.{ConfluenceFixture, ConfluenceToken}

abstract class ConfluenceSpecBase
  extends TestKit(ActorSystem("ConfluenceSpec"))
  with FunSpecLike
  with ConfluenceFixture {

  def existingPage: String

//  it  ("Pretty Print ClusterInfo") {
//   // val cluster = ClusterInfo.createClusterInfo(session, GROUP)
//    //TODO fix me
//  //  PrettyPrint.prettyPrintKeyspace(cluster,"key2")
//   // assert (true)
//  }

  //Generate the confluence pages
//  it  ("Generate Confluence - Sequenctially") {
//    val allClusters = ClusterInfo.createClusterInfo(session, GROUP)
//    GenerateCassandraConfluencePages.generateAllConfluencePages (allClusters, SPACE ,GROUP, TOKEN, false)
//  }


  it  ("Confluence Test Read Page") {
    val token: Token = ConfluenceToken.getConfluenceToken (system.settings.config)

    val page: Page = new Page
    val parentPage: RemotePage = page.read(SPACE, existingPage)
    println (parentPage.getContent)
  }

  // intellij reports an incorrect error here due to 'seconds'
  //it("AKKA - generate Confluence pages") {
  //  case object Finished
  //  val controller = system.actorOf(Props[ClusterInfoController])
  //
  //  val actorRef = TestActorRef(new Actor {
  //    def receive = {
  //      case clusterInfo: ConfluencePage.GenerateAll => {
  //        println ("DONE!!!")
  //        GenerateCassandraConfluencePages.generateAllConfluencePages (clusterInfo.allClusters, SPACE, GROUP, TOKEN, false )
  //        //reply to testActor to keep test running until finished!!!
  //        testActor ! Finished
  //      }
  //    }
  //  })
  //  controller ! All (actorRef, GROUP, session)
  //  import scala.concurrent.duration._
  //  expectMsg(600 seconds,Finished)
  //  //TODO shutdown system
  //}



  //  it ("test graphite api") {
  //    var prefix: String = ""
  //    for (x <- 1 to 20) {
  //      val url = s"http://graphite.europe.intranet/metrics/expand?query=${prefix}LLDS.Cassandra.*"
  //      val result = scala.io.Source.fromURL(url).mkString
  //      //println(url + " " + result)
  //      val js = Json.parse(result)
  //      println(url.toString + " " + (js \\ "text").toList)
  //      println(js)
  //      prefix = prefix + "*."
  //    }
  //  }



}



