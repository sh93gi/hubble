package team.supernova.confluence

import org.scalatest.{FunSpecLike, _}
import team.supernova.opscenter.CassandraYaml

import scala.xml.NodeSeq

class CassandraYamlSectionSpec
  extends FunSpecLike with Matchers{

  describe("presentYamlShort"){
    describe("filterPasswordLines"){
      it ("should be able to remove full line"){
        CassandraYamlSection.filterPasswordLines("a password = start123") should equal("")
      }
      it ("should be able to remove middle line"){
        CassandraYamlSection.filterPasswordLines("first line\na password = start123\nsecond line") should equal("first line\nsecond line")
      }
      it ("should be able to remove case insensitive line"){
        CassandraYamlSection.filterPasswordLines("pAsSwOrD = start123") should equal("")
      }
    }

    describe("joinWithBr"){
      it("should join two non empty seqs"){
        Confluence.joinWithBr(xml.Text("a"), xml.Text("b")).toString() should equal ("a<br/>b")
      }
      it("should not join with empty seqs"){
        Confluence.joinWithBr(xml.Text("a"), NodeSeq.Empty).toString() should equal ("a")
        Confluence.joinWithBr(NodeSeq.Empty, xml.Text("b")).toString() should equal ("b")
      }
    }

    describe("yamlValueToString"){
      it("should print string"){
        CassandraYamlSection.yamlValueToString("string value") should equal (""""string value"""")
      }
      it("should print int"){
        CassandraYamlSection.yamlValueToString(5) should equal ("5")
      }
      it("should print double"){
        CassandraYamlSection.yamlValueToString(5.5) should equal ("5.5")
      }
      it("should print map"){
        val pretty = CassandraYamlSection.yamlValueToString(Map(("a", 1), ("b",2)))
        pretty.lines.map(_.trim).toList should contain allOf ("\"a\" : 1,", "\"b\" : 2")
      }
      it("should print list"){
        CassandraYamlSection.yamlValueToString(List(1, 2, 3)) should equal ("[ 1, 2, 3 ]")
      }
    }

    describe("presentYamlShort"){
      it("should print something"){
        val content = CassandraYamlSection.presentYamlShort(Some(CassandraYaml(Map(("a", 1), ("b",2))))).toString()
        content should include ("\"a\" : 1")
      }
    }

    describe("presentYamlCompare"){
      it("should print something"){
        val content = CassandraYamlSection.presentYamlShort(Some(CassandraYaml(Map(("a", 1), ("b",2))))).toString()
        content should include ("\"a\" : 1")
      }
    }
  }

}
