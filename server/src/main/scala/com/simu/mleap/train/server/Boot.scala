package com.simu.mleap.train.server

import akka.actor.ActorSystem

object Boot extends App {
  val system = ActorSystem("ModelServing")
  MleapServer(system)
}
