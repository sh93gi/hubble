package team.supernova.confluence

import team.supernova.graphite.{GraphitePlotConfig, StringTemplate}

import scala.xml.Utility._
import scala.xml._

object GraphitePlotSection {
  def present(graphiteConfig: GraphitePlotConfig, templateArgs:Map[String, String]): NodeSeq = {
    val clusterGraphUrl = new StringTemplate(graphiteConfig.url_template).fillWith(templateArgs)

    <p><h1>{Text(graphiteConfig.header)}</h1>
      <a href={ escape(clusterGraphUrl) }><img src={ escape(clusterGraphUrl) }/></a>
    </p>
  }

}
