package com.simu.mleap.train.server

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Month}

import com.simu.mleap.train.server.support._
import ml.combust.bundle.BundleFile
import ml.combust.bundle.dsl.Bundle
import ml.combust.mleap.core.types.{ScalarType, StructField, StructType}
import ml.combust.mleap.runtime.MleapSupport._
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, Row, Transformer}
import resource._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

class MleapService()(implicit ec: ExecutionContext) {
  private var bundle: Option[Bundle[Transformer]] = None
  private val format = DateTimeFormatter.ofPattern("dd-MM-yyyy")
  private val items = Source.fromFile("/tmp/items.csv")
    .getLines.drop(1)
    .map(_.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"))
    .map(e => e(1).toLong -> e(2).toLong)
    .toMap
  private val sales = Source.fromFile("/tmp/sales_train.csv")
    .getLines.drop(1)
    .map(_.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"))
    .map(e => e(3).toLong -> e(4).toDouble)
    .toMap

  def setBundle(bundle: Bundle[Transformer]): Unit = synchronized(this.bundle = Some(bundle))

  def unsetBundle(): Unit = synchronized(this.bundle = None)

  def loadModel(request: LoadModelRequest): Future[LoadModelResponse] = Future {
    (for (bf <- managed(BundleFile(new File(request.path.get.toString)))) yield {
      bf.loadMleapBundle()
    }).tried.flatMap(identity)
  }.flatMap(r => Future.fromTry(r)).andThen {
    case Success(b) =>
      setBundle(b)
      println("loaded with success")
    case Failure(f) =>
      println("faillure")
      f.printStackTrace()
  }.map(_ => LoadModelResponse())

  def unloadModel(request: UnloadModelRequest): Future[UnloadModelResponse] = {
    unsetBundle()
    Future.successful(UnloadModelResponse())
  }

  def transform(frame: ModelRequest): Try[DefaultLeapFrame] = synchronized {
    // item_price,item_cnt_day,date_block_num
    val schema: StructType = StructType(
      StructField("shop_id", ScalarType.Long),
      StructField("item_id", ScalarType.Long),
      StructField("item_category_id", ScalarType.Long),
      StructField("item_price", ScalarType.Double),
      StructField("date_block_num", ScalarType.Int)).get

    val accessor = format.parse(frame.dateDay)

    val dataset = Seq(Row(
      frame.shopId,
      frame.itemId,
      items.getOrElse(frame.itemId, 0l),
      sales.getOrElse(frame.itemId, 0d),
      frame.dateBock))

    val leapFrame = DefaultLeapFrame(schema, dataset)

    bundle.map {
      _.root.transform(leapFrame)
    }.getOrElse(Failure(new IllegalStateException("no transformer loaded")))
  }

  def getSchema: Try[StructType] = synchronized {
    bundle.map {
      bundle => Success(bundle.root.schema)
    }.getOrElse(Failure(new IllegalStateException("no transformer loaded")))
  }
}
