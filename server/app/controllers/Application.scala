package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

import com.virtuslab.akkaworkshop._

object Application extends Controller {

  implicit val timeout = Timeout(5.seconds)

  def index = Action {
    Ok(views.html.index())
  }

  def leaderboard = Action {
    import PasswordsDistributor._
    val distributor = Akka.system.actorSelection("akka.tcp://application@headquarters:9552/user/PasswordsDistributor")
    val statistics = Await.result(distributor ? SendMeStatistics, timeout.duration).asInstanceOf[Statistics]
    Ok(views.html.leaderboard(statistics.clients))
  }
}