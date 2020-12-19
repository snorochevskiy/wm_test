package snorochevskiy.wm

import akka.actor.typed.ActorSystem

object Main {

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[Any] = ActorSystem(MwApplication(), "wm-application")
  }
}
