package snorochevskiy.wm

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.typed.ActorSystem
import org.awaitility.Awaitility
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.Using

class MwApplicationTest extends AnyWordSpec with should.Matchers {

  "Throttled forwarder app" must {
    "perform parallel forwarding" in {

      withApp {
        implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
        val futures = 0.to(5)
          .map(_ => Future { call("http://localhost:8080/get-fortune") })
          .toList

        val responses = Await.result(Future.sequence(futures), Duration.create(20, TimeUnit.SECONDS))
        responses.forall(_.nonEmpty) shouldBe true
      }

    }
  }

  private def withApp[T]( f: =>T): T = {
    val system: ActorSystem[Any] = ActorSystem(MwApplication(), "wm-application")
    try {
      waitUntilServerIsUp()
      f
    } finally {
      system.terminate()
    }
  }

  private def waitUntilServerIsUp(): Unit = {
    Awaitility.`with`.pollInterval(java.time.Duration.ofSeconds(1))
      .and().`with`().pollDelay(java.time.Duration.ofSeconds(3))
      .and().`with`().atMost(java.time.Duration.ofSeconds(10))
      .await("customer registration")
      .until(()=>call("http://localhost:8080/health").nonEmpty)
  }

  private def call(url: String): String = {
    val start = System.currentTimeMillis()
    val response = Using.resource(Source.fromURL(url)){ src =>
      src.mkString
    }
    val end = System.currentTimeMillis()
    println("took " + (end - start) + " milliseconds : " + response)
    response
  }
}
