package team.supernova

import java.io.InputStream
import java.net.URL
import java.util.Base64

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
      url should include (clusterInstance.graphite)
    }

    it  ("should generate url with lowercase cluster name") {
      val url = graphiteApi.clusterGraphUrl(clusterInstance.cluster_name)
      url should include (clusterInstance.cluster_name.toLowerCase)
    }

    def authorizedInputStream(url: String ): InputStream ={
      val name = graphiteUserName
      val password = graphitePassword
      val authString = name + ":" + password
      val authEncBytes = Base64.getEncoder.encode(authString.getBytes())
      val authStringEnc = new String(authEncBytes)

      val url_ref = new URL(url)
      val urlConnection = url_ref.openConnection()
      urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc)
      val is = urlConnection.getInputStream()
      is
    }

    def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
      try {
        f(param)
      } finally {
        param.close()
      }

    it  ("should create img with data (behind http authorization)") {
      // using heuristic of reasonably sized png (4KB returned for 'no data', 32KB for 'with data')
      val url = graphiteApi.clusterGraphUrl(clusterInstance.cluster_name)
      import javax.imageio.ImageIO
      withClue(url){
        using(authorizedInputStream(url)) { isr => {
          val img = ImageIO.read(isr)
          img should not be (null)
          img.getHeight should be (250)
          img.getWidth should be (400)
          val rgb = img.getRGB(10, 10, 10, 10, null, 0, 10)
          val min = rgb.min
          val max = rgb.max
          //When it is a 'no data' image, the small excerpt of the image will be constant color
          min should not be (max)
          }
        }
      }
    }
  }
}
