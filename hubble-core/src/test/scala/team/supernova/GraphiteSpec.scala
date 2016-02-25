package team.supernova

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, FunSpecLike}
import Matchers._

class GraphiteSpec
    extends TestKit(ActorSystem("ConfluenceSpec"))
      with FunSpecLike
      with GraphiteFixture {

  describe("Graphite graph retriever"){
    it  ("should generate url") {
      val url = graphiteApi.clusterGraphUrl(clusterInstance.cluster_name)
      url should startWith ("http")
      url should include (clusterInstance.cluster_name)
      url should include (clusterInstance.graphite)
    }
    def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
      try {
        f(param)
      } finally {
        param.close()
      }

    it  ("should create img with data") {
      // using heuristic of reasonably sized png (4KB returned for 'no data', 32KB for 'with data')
      val url = graphiteApi.clusterGraphUrl(clusterInstance.cluster_name)
      import javax.imageio.ImageIO
      withClue(url){
        val img = ImageIO.read(new URL(url))
        img should not be (null)
        img.getHeight should be (250)
        img.getWidth should be (400)
        img.toString.length should be >= (500)
      }
    }
  }
}
