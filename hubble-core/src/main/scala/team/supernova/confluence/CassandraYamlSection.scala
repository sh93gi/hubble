package team.supernova.confluence

import java.io.StringWriter

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import team.supernova.domain.CassandraYaml

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.xml.{NodeSeq, Text}

object CassandraYamlSection {
  def presentYamlCompare(yamls : List[(String, Option[CassandraYaml])]) : NodeSeq = {
    val yamlAvailable = yamls.filter(_._2.isDefined).map(kv=>(kv._1, kv._2.get))
    if (yamlAvailable.isEmpty)
      return NodeSeq.Empty
    // get keys, ordered by number unique values
    // first collect all (key, value) pairs
    val keys = yamlAvailable.flatMap(_._2.all.toList)
        .groupBy(_._1)  // group by key of (key, value) tuple
        .mapValues(pairs=>pairs.map(_._2).toSet.size - 1) // map list if (key, value) to count of different values
        .toList.sortBy(kv=>(-kv._2, kv._1)) // create descending sorted list, first by nr values, then by name
    val nodeNames = yamlAvailable.sortBy(_._1)
    def joinWithBr(a: NodeSeq, b:NodeSeq)={
      a.++(xml.Unparsed("<br/>")).++(b)
    }
    <h1>Node Yaml comparison</h1>
      <p>
        <table>
          <tbody><tr><th>Yaml key</th><th>nr different values</th>{ nodeNames.map(nameYaml=> <th>{Text(nameYaml._1)}</th>) }</tr>
            {
            keys.map(keyCount=>{
              val key = keyCount._1
              val valueCount = keyCount._2
              <tr>
                <td>{Text(key)}</td>
                <td>{valueCount}</td>{
                nodeNames.map(nameYaml=>
                  <td>{
                    nameYaml._2.all.get(key).map(yamlValueToString).getOrElse("").split("\r\n").map(s=>Text(s))
                      .reduce(joinWithBr)
                    }</td>
                ).toSeq
                }
              </tr>
            })
            }
          </tbody>
        </table>
      </p>
  }

  def deepAsJava(value:Any):Any={
    value match{
      case seq: ::[Any] if seq.size==1 => seq.map(deepAsJava).head
      case seq: ::[Any] if seq.nonEmpty => seq.map(deepAsJava).asJava
      case seq: ::[Any] if seq.isEmpty => List().asJavaCollection
      case map: collection.Map[String, Any] => mapAsJavaMap(map.mapValues(deepAsJava))
      case list: collection.immutable.List[Any] => list.map(deepAsJava).asJava
      case list: collection.mutable.LinearSeq[Any] => list.map(deepAsJava).asJava
      case seq: Seq[Any] => seqAsJavaList(seq.map(deepAsJava))
      case set: Set[Any] => setAsJavaSet(set.map(deepAsJava))
      case it: Iterable[Any] => asJavaCollection(it.map(deepAsJava))
      case _=> value
    }
  }

  def yamlValueToString(value: Any): String = {
    if (value==null)
      "null / None / N/A"
    else {
        try {
          implicit val formats = DefaultFormats
          val out = new StringWriter
          mapper.writeValue(out, deepAsJava(value))
          val json = out.toString
          pretty(parse(json))
        }
        catch {
          case e: Exception =>
            value.toString.replaceAllLiterally(",", ",\r\n")
        }
    }
  }

  def presentYamlShort(opsCenterNode: Option[CassandraYaml]): NodeSeq = {
    if (opsCenterNode.isEmpty)
      return NodeSeq.Empty
    implicit val formats = DefaultFormats
    val yaml = try {
      pretty(parse(write(opsCenterNode.get)))
    }
    catch {
      case e: Exception => ""
    }
    Confluence.confluenceCodeBlock("Yaml", yaml, "none")
  }
}
