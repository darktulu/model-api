package com.simu.mleap.train.server

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import com.simu.mleap.train.server.marshalling.ApiMarshalling._
import com.simu.mleap.train.server.marshalling.LeapFrameMarshalling._
import com.simu.mleap.train.server.support.{LoadModelRequest, ModelRequest, UnloadModelRequest}

class MleapResource(service: MleapService)
                   (implicit system: ActorSystem) {
  private def recordLog(logger: LoggingAdapter, level: LogLevel)(req: HttpRequest)(res: RouteResult): Unit = res match {
    case RouteResult.Complete(x) =>
      logger.log(level, logger.format("status={} scheme=\"{}\" path=\"{}\" method=\"{}\"",
        x.status.intValue(), req.uri.scheme, req.uri.path, req.method.value))
    case RouteResult.Rejected(rejections) => // no logging required
  }

  def errorHandler(logger: LoggingAdapter): ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      logger.error(e, "error with request")
      complete((StatusCodes.InternalServerError, e))
  }

  val logger = Logging.getLogger(system.eventStream, classOf[MleapResource])

  val routes: Route = handleExceptions(errorHandler(logger)) {
    withLog(logger) {
      logRequestResult(LoggingMagnet(recordLog(_, Logging.InfoLevel))) {
        path("model") {
          put {
            entity(as[LoadModelRequest]) {
              request => complete(service.loadModel(request))
            }
          } ~ delete {
            complete(service.unloadModel(UnloadModelRequest()))
          } ~ get {
            complete(service.getSchema)
          }
        } ~ path("transform") {
          post {
            entity(as[ModelRequest]) {
              frame => complete(service.transform(frame))
            }
          }
        }
      }
    }
  }
}
