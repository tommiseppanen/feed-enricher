package feedenricher

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectResult
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.xml.{Elem, Node, Unparsed, XML}

class FeedEnricher {
  val bucketName = "insert-your-bucket-name-to-here"
  val configurationFilePath = "config/sources.json"
  val outputPath = "feeds/"
  val amazonS3Client = new AmazonS3Client()

  def execute(event: S3Event): java.util.List[String] = {
    println("Starting...")
    val json: JsValue = Json.parse(readConfig())
    json.as[List[FeedSource]].par.foreach(s => processFeed(s.name, s.url, s.selectors))
    println("Finished")
    null
  }

  def readConfig(): String = {
    amazonS3Client.getObjectAsString(bucketName, configurationFilePath)
  }

  def processFeed(name: String, url: String, selectors: List[String]) = {
    val xml = XML.load(url)
    val updated = handleFeedItems(selectors)(xml)
    writeFile(name, updated.toString())
  }

  def handleFeedItems(selectors: List[String])(node: Node): Node = {
    node match {
      case elem @ Elem(_, "item", _, _, child @ _*) =>
        elem.asInstanceOf[Elem].copy(child = child map updateDescription(selectors, (elem \ "link").head.text))
      case elem @ Elem(_, _, _, _, child @ _*) =>
        elem.asInstanceOf[Elem].copy(child = child map handleFeedItems(selectors))
      case other => other
    }
  }

  def updateDescription(selectors: List[String], url: String)(node: Node): Node = {
    node match {
      case elem @ Elem(_, "description", _, _, child @ _*) =>
        elem.asInstanceOf[Elem].copy(child = new Unparsed("<![CDATA["+extractContent(url, selectors)+"]]>"))
      case other => other
    }
  }

  def extractContent(url: String, selectors: List[String]): String = {
    val browser = JsoupBrowser()
    val doc = browser.get(url)
    val content = selectors.flatMap(e => doc >> elementList(e))
    val markup = content.map(e => e.innerHtml).mkString("")
    markup.replaceAll("""<!\[CDATA\[""", "").replaceAll("""\]\]>""","")
  }

  def writeFile(name: String, content: String): PutObjectResult = {
    amazonS3Client.putObject(bucketName, outputPath+name, content)
  }

  case class FeedSource(name: String, url: String, selectors: List[String], exclude: List[String])
  implicit val feedSourceReader: Reads[FeedSource] = (
    (__ \ "name").read[String] and
      (__ \ "url").read[String] and
      (__ \ "selectors").read[List[String]] and
      (__ \ "exclude").read[List[String]]
    ) (FeedSource.apply _)
}
