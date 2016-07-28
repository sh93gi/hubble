package team.supernova.confluence

import team.supernova.results.MaturityAspect

import scala.xml.{NodeSeq, Text}

object MaturitySection {
  def present(header: String, maturities:Map[Int, List[MaturityAspect]]): NodeSeq ={
    <p><h2>{Text(header)}</h2>++
      { maturities.toList.sortBy(_._1).map { case (level, aspects) => presentLevel(level, aspects) } }++
    </p>
  }

  def presentLevel(level: Int, aspects: List[MaturityAspect]): NodeSeq = {
    val base = aspects.map(_.weight).sum
    val ratio = aspects.filter(_.check.hasPassed).map(_.weight).sum / base
    val failed = aspects.filterNot(_.check.hasPassed).map {
      case aspect => "for %.2f%%: %s".format(100D*aspect.weight/base, aspect.check.details)
      }.sorted.mkString("\n")
    <h3>Maturity level {level} : {"%.2f".format(100D*ratio)}%</h3>++
      { Confluence.confluenceCodeBlock("Failed aspects", failed ,"none") }++
        <br/>
  }

}
