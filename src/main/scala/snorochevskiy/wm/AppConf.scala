package snorochevskiy.wm

import com.typesafe.config.{Config, ConfigFactory}

case class ThrottledForwardingConf(backends: List[String])
case class ServerConf(host: String, port: Int)

object AppConf {
  def load(): Config = ConfigFactory.load()

  implicit class AppConfig(val conf: Config) extends AnyVal{
    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._

    def throttledForwardingConf: ThrottledForwardingConf =
      conf.as[ThrottledForwardingConf]("throttled-forwarding")

    def serverConf: ServerConf =
      conf.as[ServerConf]("web-server")
  }
}
