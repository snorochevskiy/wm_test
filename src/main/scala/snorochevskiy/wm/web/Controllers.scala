package snorochevskiy.wm.web

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives.{extractUri, handleExceptions, onSuccess, path}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import snorochevskiy.wm.{AppConf, ServerConf}
import snorochevskiy.wm.AppConf.AppConfig
import snorochevskiy.wm.actors.SyncCallActor.{GetReq, Resp}

import scala.concurrent.duration.DurationInt

/**
 * WebServer definition.
 * @param system
 * @param bufferedThrottle
 */
class Controllers(system: ActorSystem[_], bufferedThrottle: ActorRef[GetReq]) {

  def startServer(): Unit = {
    implicit val actorSystem = system
    implicit val executionContext = system.executionContext

    val ServerConf(host, port) = AppConf.load().serverConf
    val bindingFuture = Http().newServerAt(host, port).bind(routes)

    bindingFuture
      .map(serverBinding => actorSystem.log.info(s"RestApi bound to ${serverBinding.localAddress} "))
      .failed.foreach {
        case e: Exception =>
          actorSystem.log.error("Failed to start web server: {}", e)
          system.terminate()
      }
  }

  def routes: Route = handleExceptions(exceptionHandler) {
    getFortuneEndpoint ~ healthEndpoint
  }

  private val getFortuneEndpoint: Route =
    path("get-fortune") {
      Directives.get {
        // Even people who can't leave without fortune predictions/advices won't wait for 30 seconds
        implicit val timeout: Timeout = 30.seconds
        implicit val ec = system.executionContext
        implicit val scheduler = system.scheduler
        val fut = (bufferedThrottle ? (GetReq("get-fortune", _))).mapTo[Resp]
        onSuccess(fut) { r=>
          complete(HttpEntity(r.contentType, r.payload))
        }
      }
    }

  private val healthEndpoint: Route =
    path("health") {
      Directives.get {
        complete(HttpEntity("OK"))
      }
    }

  private val exceptionHandler = ExceptionHandler {
    case e: Exception =>
      system.log.warn("Unexpected error: {}", e.getMessage)
      extractUri { uri =>
        complete(InternalServerError, e.getMessage)
      }
  }
}

