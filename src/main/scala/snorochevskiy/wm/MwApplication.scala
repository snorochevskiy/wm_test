package snorochevskiy.wm

import akka.actor.typed.{Behavior, DispatcherSelector, SupervisorStrategy}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import snorochevskiy.wm.AppConf.AppConfig
import snorochevskiy.wm.actors.SyncCallActor.GetReq
import snorochevskiy.wm.actors.SyncCallActor
import snorochevskiy.wm.web.Controllers

/**
 * Top level actor, that creates buffered throttled forwarder and web server.
 */
object MwApplication {

  def apply(): Behavior[Any] =
    Behaviors.setup{ context =>
      context.log.info(s"Creating root application actor: ${context.self.path}")

      val backends = AppConf.load().throttledForwardingConf.backends
      val backendsIt = backends.iterator

      // TODO-XXX: Looks like balancing pool has been removed from akka typed, and round robin is less effective here
      val callRouter = Routers.pool[GetReq](backends.length)(Behaviors.supervise(Behaviors.setup { ctx: ActorContext[GetReq] =>
        val backendUrl = backendsIt.next()
        ctx.log.info(s"Creating forwarder=${ctx.self.path} for address =${backendUrl}")
        SyncCallActor(backendUrl)
      }).onFailure[Exception](SupervisorStrategy.resume))

      val bufferedThrottle = context.spawn(callRouter, "SyncCallPool", DispatcherSelector.fromConfig("forwarder-dispatcher"))

      new Controllers(context.system, bufferedThrottle).startServer()

      Behaviors.empty
    }

}

