package com.simu.mleap.train.server.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import JsonSupport._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.truecar.mleap.demo.server.support._

/**
  * Created by hollinwilkins on 1/30/17.
  */
trait ApiMarshalling {
  implicit val mleapLoadModelRequestEntityUnmarshaller: FromEntityUnmarshaller[LoadModelRequest] = mleapLoadModelRequestFormat
  implicit val mleapUnloadModelRequestEntityUnmarshaller: FromEntityUnmarshaller[UnloadModelRequest] = mleapUnloadModelRequestFormat

  implicit val mleapLoadModelResponseEntityMarshaller: ToEntityMarshaller[LoadModelResponse] = mleapLoadModelResponseFormat
  implicit val mleapUnloadModelResponseEntitytMarshaller: ToEntityMarshaller[UnloadModelResponse] = mleapUnloadModelResponseFormat

  implicit val mleapUnloadModelResponseEntitytMarshaller2: FromEntityUnmarshaller[ModelRequest] = mleapUnloadModelRequestFormat2
}
object ApiMarshalling extends ApiMarshalling
