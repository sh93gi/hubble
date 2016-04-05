package team.supernova.graphite

class StringTemplate(templateString: String) {

  /**
    * Constructs a concrete string
    * For each key, value in the provided template, replaces ${key} with value in the templateString of this GraphiteApi
    * @param template_args the dictionary using which to fill in the template
    * @return the concreate graphite url
    */
  def fillWith(template_args : Map[String, String]): String = {
    var url = templateString
    for (kv <- template_args){
      url = url.replaceAllLiterally("${"+kv._1+"}", kv._2)
    }
    url
  }
}
