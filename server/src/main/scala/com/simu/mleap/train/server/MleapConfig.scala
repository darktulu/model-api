package com.simu.mleap.train.server

import com.typesafe.config.Config

case class HttpConfig(config: Config) {
  val bindHostname = config.getString("bind-hostname")
  val bindPort = config.getInt("bind-port")
}

case class MleapConfig(config: Config) {
  val http = HttpConfig(config.getConfig("http"))
  val model = if (config.hasPath("model")) Some(config.getString("model")) else None
}
