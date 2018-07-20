package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import akka.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.CustomExecutionContext
import scala.concurrent.ExecutionContext // Future, blocking}
import play.api.libs.json._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
//import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
//import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
//import net.ruippeixotog.scalascraper.model._



trait MyExecutionContext extends ExecutionContext

class MyExecutionContextImpl @Inject()(system: ActorSystem)
  extends CustomExecutionContext(system, "blocking-pool") with MyExecutionContext


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
//class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
class HomeController @Inject()(config: Configuration, cc: ControllerComponents, blockingEC: MyExecutionContextImpl, lifecycle: ApplicationLifecycle) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
  def login() = Action { implicit request: Request[AnyContent] =>
    //val limit: Int = request.getQueryString("limit").getOrElse(request.toString()).toInt
    val date = System.currentTimeMillis()
    val urlRobotica = "http://www.aprenderobotica.com/main/authorization/doSignIn?target=http%3A%2F%2Fwww.aprenderobotica.com%2F"
    val formRobotica = Map("emailAddress" -> "PPPP", "password" -> "XXXX","xg_token" -> "")
    val url=urlRobotica
    val form=formRobotica
    //-------------------------------------------------------------------------------
    val UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0"
    val browser = new JsoupBrowser(UserAgent)
    //val browser = HtmlUnitBrowser()
    //-------------------------------------------------------------------------------
    val doc = browser.post(url,form)
    val session = browser.cookies("http://www.aprenderobotica.com")("xn_id_roboticaeducativa")
    val sessionText = s"Cookie de Sesión: Session->$session"
    //-------------------------------------------------------------------------------
    val username = doc >> text("#xn_username")
    val usernameText = s"Parseando Aprende Robotica: Usuario->$username "
    //-------------------------------------------------------------------------------
    //val token = doc >> attr("value")("input[name=xg_token]")
    val token = (doc >> element("input[name=xg_token]")).attr("value")
    val tokenText = s"Token de seguridad:$token"
    //-------------------------------------------------------------------------------
    //val urllogout= "http://www.aprenderobotica.com/main/authorization/signOut?target=http%3A%2F%2Fwww.aprenderobotica.com%2F&xg_token="+ token
    //val exit = browser.get(urllogout)
    //-------------------------------------------------------------------------------
    val dura1 = (System.currentTimeMillis()-date).toString
    val dura1Text = s"Duración total en ms:$dura1"
    val json: JsValue = Json.obj(
      "username" -> username,
      "token" -> token,
      "session" -> session,
      "usernameText" -> usernameText,
      "tokenText" -> tokenText,
      "sessionText" -> sessionText,
      "audit" -> dura1Text
    )
    Ok(json)
  }

  def query(tokenst: String): JsValue = {
    val date = System.currentTimeMillis()
    val urlRobotica = "http://www.aprenderobotica.com"
    //val formRobotica = Map("xg_token" -> tokenst)
    val url=urlRobotica
    //val form=formRobotica
    //-------------------------------------------------------------------------------
    val UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0"
    val browser = new JsoupBrowser(UserAgent)
    browser.setCookie(urlRobotica,"xn_id_roboticaeducativa",tokenst)
    var errorStatus = 0
    //-------------------------------------------------------------------------------
    val doc = browser.get(url)
    val session = browser.cookies("http://www.aprenderobotica.com")("xn_id_roboticaeducativa")
    val sessionText = s"Cookie de Sesión: Session->$session"
    //-------------------------------------------------------------------------------
    val items = doc >> elementList("div[class=xg_module_body body_detail] h3")
    val sal=items.map(_.text)
    //-------------------------------------------------------------------------------
    val username = doc >?> text("#xn_username") match {
      case Some(s) => s
      case None =>
        errorStatus = 1
        "no logado"
    }
    val usernameText = s"Parseando Aprende Robotica: Usuario->$username "
    //-------------------------------------------------------------------------------
    //val token = doc >> attr("value")("input[name=xg_token]")
    val token = doc >?> element("input[name=xg_token]") match {
      case Some(s) => s.attr("value")
      case None =>
        errorStatus = 1
        "no logado"
    }
    val tokenText = s"Token de seguridad:$token"
    //-------------------------------------------------------------------------------
    val dura1 = (System.currentTimeMillis()-date).toString
    val dura1Text = s"Duración total en ms:$dura1"
    val json: JsValue = if (errorStatus == 0)
      Json.obj(
        "blogs" -> Json.toJson(sal),
        "username" -> username,
        "token" -> token,
        "session" -> session,
        "usernameText" -> usernameText,
        "tokenText" -> tokenText,
        "audit" -> dura1Text
      )
    else
      Json.obj(
        "blogs" -> List("Error al recuperar la página"),
        "username" -> username,
        "session" -> session,
        "audit" -> dura1Text
      )
    json
  }
  def queryget() = Action { implicit request: Request[AnyContent] =>
    val tokenst = request.getQueryString("session").getOrElse(request.toString())
    val json = query(tokenst)
    Ok(json)
  }
  def querypost() = Action { implicit request: Request[AnyContent] =>
    val tokenst = request.body.asFormUrlEncoded.get.get("session").head.toString()
    val json = query(tokenst)
    Ok(json)
  }

  def logout(tokenst: String):JsValue =  {
    val date = System.currentTimeMillis()
    val urlRobotica = "http://www.aprenderobotica.com/main/authorization/signOut"//?target=http%3A%2F%2Fwww.aprenderobotica.com%2F&xg_token=6d384f15acc63ef9ed55f19d7b840159"
    val url=urlRobotica
    //-------------------------------------------------------------------------------
    val UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0"
    val browser = new JsoupBrowser(UserAgent)
    browser.setCookie(urlRobotica,"xn_id_roboticaeducativa",tokenst)
    //val browser = HtmlUnitBrowser()
    //-------------------------------------------------------------------------------
    browser.get(url)
    //-------------------------------------------------------------------------------
    //val urllogout= "http://www.aprenderobotica.com/main/authorization/signOut?target=http%3A%2F%2Fwww.aprenderobotica.com%2F&xg_token="+ token
    //val exit = browser.get(urllogout)
    //-------------------------------------------------------------------------------
    val dura1 = (System.currentTimeMillis()-date).toString
    val dura1Text = s"Duración total en ms:$dura1"
    val json: JsValue = Json.obj(
      "status" -> "Logout correcto",
      "audit" -> dura1Text
    )
    json
  }
  def logoutget() = Action { implicit request: Request[AnyContent] =>
    val tokenst = request.getQueryString("session").getOrElse(request.toString())
    val json = logout(tokenst)
    Ok(json)
  }
  def logoutpost() = Action { implicit request: Request[AnyContent] =>
    val tokenst = request.body.asFormUrlEncoded.get.get("session").head.toString()
    val json = logout(tokenst)
    Ok(json)
  }

  def getnotice() = Action { implicit request: Request[AnyContent] =>
    val browser = JsoupBrowser()
    val doc = browser.parseFile("public/test/resource/example.html")
    val docheader = doc >> text("#header")
    //val doc2 = browser.get("http://example.com")
    val json = Json.toJson(s"Parseado de ejemplo: Cabecera->$docheader ")
    Ok(json)
  }
}
