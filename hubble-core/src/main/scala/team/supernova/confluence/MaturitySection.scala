package team.supernova.confluence

import team.supernova.results.MaturityAspect

import scala.xml.{NodeSeq, Text}

object MaturitySection {
  def present(header: String, maturities:Map[Int, List[MaturityAspect]]): NodeSeq ={
    <p><h2>{Text(header)}</h2>
      { maturities.toList.sortBy(_._1).map { case (level, aspects) => presentLevel(level, aspects) } }
    </p>
  }

  def formatCheck(aspectWeight: Double, details: String, base: Double):String={
    "for %.2f%%: %s".format(100D*aspectWeight/base, details)
  }

  def presentLevel(level: Int, aspects: List[MaturityAspect]): NodeSeq = {
    val base = aspects.map(_.weight).sum
    val ratio = aspects.filter(_.check.hasPassed).map(_.weight).sum / base
    val failed = aspects.filterNot(_.check.hasPassed).map{ case aspect=>formatCheck(aspect.weight, aspect.check.details, base)}.sorted.mkString("\n")
    val succeeded = aspects.filter(_.check.hasPassed).map{ case aspect=>formatCheck(aspect.weight, aspect.check.name, base)}.sorted.mkString("\n")
    <p><h3>Maturity level {level} : {"%.2f".format(100D*ratio)}%</h3>
      { Confluence.confluenceCodeBlock("Failed aspects", failed ,"none") }
      { Confluence.confluenceCodeBlock("Succeeded aspects", succeeded ,"none") }
        <br/></p>
  }

}
