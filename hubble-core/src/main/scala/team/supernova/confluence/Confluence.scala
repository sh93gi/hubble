package team.supernova.confluence

import org.slf4j.LoggerFactory
import team.supernova.confluence.soap.rpc.soap.actions.Page
import team.supernova.confluence.soap.rpc.soap.beans.RemotePage

import scala.xml.NodeSeq

/**
 * Created by Gary Stewart on 4-8-2015.
 *
 */

object Confluence {
  val log = LoggerFactory.getLogger(Confluence.getClass)

  def CONFLUENCE_HEADER (intro: String) = <ac:structured-macro ac:name="section">
    <ac:rich-text-body>
      <ac:structured-macro ac:name="column">
        <ac:parameter ac:name="width">50%</ac:parameter> <ac:rich-text-body>
        <h1>General information</h1>
        <ac:structured-macro ac:name="warning">
          <ac:parameter ac:name="title">GENERATED CODE!!!</ac:parameter>
          <ac:rich-text-body><p>NB!!! This page is generated based on information from Cassandra. PLEASE DON'T EDIT IT!!!</p></ac:rich-text-body>
        </ac:structured-macro>
        <p>{intro}</p>
      </ac:rich-text-body>
      </ac:structured-macro> <ac:structured-macro ac:name="column">
      <ac:parameter ac:name="width">50%</ac:parameter> <ac:rich-text-body>
        <ac:structured-macro ac:name="panel">
          <ac:parameter ac:name="title">What's on this page</ac:parameter> <ac:parameter ac:name="borderStyle">solid</ac:parameter> <ac:rich-text-body>
          <p>
            <ac:structured-macro ac:name="toc">
              <ac:parameter ac:name="maxLevel">4</ac:parameter>
            </ac:structured-macro>
          </p>
        </ac:rich-text-body>
        </ac:structured-macro>
      </ac:rich-text-body>
    </ac:structured-macro>
    </ac:rich-text-body>
  </ac:structured-macro>


  def md5hash(s: String) = {
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  //update a page if the token is unknown or create a confluence page if the page does not exist
  def confluenceCreatePage(project: String, pageName: String, content: String, pageObject: Page, parentPage: RemotePage, tokenPage: RemotePage, notify: Boolean=true): String = {

    val contentMD5 = pageName + "_MD5:" + md5hash(content)

    //if the contentMD5 exists in TOKEN no need to read and update confluence!

      try {
        log.info(s"Checking ${tokenPage.getTitle} for page: $pageName ")
        if (!tokenPage.getContent.contains(contentMD5)) {
          log.info(s"Updating: $pageName")
          val existingPage: RemotePage = pageObject.read(project, pageName)
          existingPage.setContent(content)
          pageObject.update(existingPage, notify)
          log.info(s"$pageName page updated, notify = $notify")
        } else {
          log.info(s"$pageName page not updated!")
        }
      }
      catch {
        case e: Exception =>
          val newPage: RemotePage = new RemotePage
          newPage.setContent(content)
          newPage.setTitle(pageName)
          newPage.setParentId(parentPage.getId)
          newPage.setSpace(parentPage.getSpace)
          pageObject.store(newPage)
          log.info(s"$pageName created!")
      }


    //return the token
    contentMD5
  }

  //https://confluence.atlassian.com/doc/expand-macro-223222352.html
  //DONT AUTO-FORMAT THIS OTHERWISE IT WILL FAIL ON CONFLUENCE!!!
  def confluenceExpandBlock(title: String, content: NodeSeq): NodeSeq = {
    if (content.toString().trim.isEmpty || content.isEmpty) {
      NodeSeq.Empty
    } else {
      <ac:structured-macro ac:name="expand">
        <ac:parameter ac:name="title">{title}</ac:parameter>
        <ac:rich-text-body>
          {content}
        </ac:rich-text-body>
      </ac:structured-macro>
    }
  }

  //https://confluence.atlassian.com/display/DOC/Code+Block+Macro
  //DONT AUTO-FORMAT THIS OTHERWISE IT WILL FAIL ON CONFLUENCE!!!
  def confluenceCodeBlock(title: String, data: String, lang: String): NodeSeq = {
    if (!data.trim.isEmpty) {
      <ac:structured-macro ac:name="code">
        <ac:parameter ac:name="title">{title}</ac:parameter>
        <ac:parameter ac:name="theme">Default</ac:parameter>
        <ac:parameter ac:name="linenumbers">false</ac:parameter>
        <ac:parameter ac:name="language">{lang}</ac:parameter>
        <ac:parameter ac:name="firstline"></ac:parameter>
        <ac:parameter ac:name="collapse">true</ac:parameter>
        <ac:plain-text-body>{scala.xml.Unparsed("<![CDATA[%s]]>".format(data))}</ac:plain-text-body>
      </ac:structured-macro>
    } else {
      NodeSeq.Empty
    }
  }

  def joinWithBr(a: NodeSeq, b:NodeSeq)={
    if (b equals NodeSeq.Empty)
      a
    else if (a equals NodeSeq.Empty)
      b
    else
      a.++(xml.Unparsed("<br/>")).++(b)
  }
}