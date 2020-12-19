package snorochevskiy.wm.util

import akka.http.scaladsl.model.ResponseEntity
import akka.stream.Materializer
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

object HttpRespUtil {

  /**
   * Read response body from akka http response entity and returns it as a byte string
   * @param entity response entity got from http call to another service
   * @param ec
   * @param m
   * @return
   */
  def payloadBytes(entity: ResponseEntity)(implicit ec: ExecutionContext, m: Materializer): Future[ByteString] = {
    entity.dataBytes.runFold(ByteString(""))(_ ++ _)
  }

}
