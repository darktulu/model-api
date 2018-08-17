package com.simu.mleap.train.server

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.simu.mleap.train.server.support.LoadModelRequest
import com.typesafe.config.Config

object MleapServer extends ExtensionId[MleapServer]
  with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MleapServer = {
    new MleapServer(system.settings.config)(system)
  }

  override def lookup(): ExtensionId[_ <: Extension] = MleapServer
}

class MleapServer(tConfig: Config)
                 (implicit val system: ExtendedActorSystem) extends Extension {

  import system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config = MleapConfig(tConfig.getConfig("ml.combust.mleap.serving"))

  val service = new MleapService()
  val resource = new MleapResource(service)
  val routes: Route = resource.routes

  for (model <- config.model) {
    service.loadModel(LoadModelRequest().withPath(model))
  }.map(_ => Http().bindAndHandle(routes, config.http.bindHostname, config.http.bindPort))
}
