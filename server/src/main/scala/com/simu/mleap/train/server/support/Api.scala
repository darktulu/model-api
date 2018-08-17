package com.simu.mleap.train.server.support

import java.util.Date

import ml.combust.mleap.runtime.frame.DefaultLeapFrame

/**
  * Created by hollinwilkins on 1/30/17.
  */
case class LoadModelRequest(path: Option[String] = None) {
  def withPath(value: String): LoadModelRequest = copy(path = Some(value))
}
case class LoadModelResponse()

case class UnloadModelRequest()
case class UnloadModelResponse()

case class TransformRequest(frame: Option[DefaultLeapFrame]) {
  def withFrame(value: DefaultLeapFrame): TransformRequest = copy(frame = Some(value))
}
case class TransformResponse(frame: DefaultLeapFrame)

case class ModelRequest(shopId: Long, itemId: Long, dateBock: Int, dateDay: String)
