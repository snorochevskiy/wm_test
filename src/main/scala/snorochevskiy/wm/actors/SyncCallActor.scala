package snorochevskiy.wm.actors

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.util.ByteString
import snorochevskiy.wm.util.HttpRespUtil

import scala.concurrent.duration.Duration
import scala.concurrent.Await

/**
 * This actor works as a synchronous forwarder calls to a backend.
 * Since calls are performed synchronously, actor itself serves as throttle,
 * and actor's mailbox works as a buffer.
 *
 * By combining multiple actors in a pool/group we getting buffered throttle with the number
 * of concurrent calls same as number of actors in pool/group.
 */
object SyncCallActor {

  final case class GetReq(url: String, replyTo: ActorRef[Resp])
  final case class Resp(contentType: ContentType, payload: ByteString)

  def apply(baseUrl: String): Behavior[GetReq] = Behaviors.receive { (context, message) =>
    implicit val system = context.system
    implicit val materializer = Materializer(system)
    implicit val executionContext = context.executionContext

    val callUrl = s"$baseUrl/${message.url}"
    context.log.info(s"${context.self.path} is calling $callUrl")

    val futResp = Http().singleRequest(HttpRequest(uri = callUrl))
      .flatMap{
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          HttpRespUtil.payloadBytes(entity).map(entity.contentType -> _)
        case HttpResponse(code, _, entity, _) =>
          context.log.warn(s"Failed with HTTP code=${code} while calling url=${message.url}")
          HttpRespUtil.payloadBytes(entity).map(entity.contentType -> _)
      }

    // By making call synchronous, call we actually use actor as a throttle
    val result = Await.result(futResp, Duration.create(10, TimeUnit.SECONDS))

    message.replyTo ! Resp.tupled(result)
    Behaviors.same
  }


}
