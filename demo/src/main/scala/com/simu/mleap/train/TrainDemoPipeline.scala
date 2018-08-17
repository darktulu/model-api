package com.simu.mleap.train

import ml.combust.bundle.BundleFile
import ml.combust.mleap.spark.SparkSupport._
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.bundle.SparkBundleContext
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import resource._

object TrainDemoPipeline extends App {
  // User defined function
  //  val date = LocalDate.of(2013, Month.JANUARY, 1)
  //  val date_int_format = DateTimeFormatter.ofPattern("dd.MM.yyyy")
  //  def toLowerFun(str: String): Option[Long] = {
  //    val accessor = date_int_format.parse(str)
  //    Some(ChronoUnit.DAYS.between(date, LocalDate.from(accessor)))
  //  }
  //  val toNewFeature = udf[Option[Long], String](toLowerFun)
  //  df = df.withColumn("date_day_num", toNewFeature(df.col("date")))

  // start spark context
  val spark = SparkSession.builder
    .appName("app")
    .master("local")
    .getOrCreate()

  // init variables and constants
  private val readOptions = Map("header" -> "true", "mode" -> "DROPMALFORMED", "nullValue" -> "")
  private val salesFile = "/tmp/sales_train.csv"
  private val itemsFile = "/tmp/items.csv"
  private val UNSCALED_CONTINUOUS_FEATURES = "unscaled_continuous_features"
  private val SCALED_CONTINUOUS_FEATURES = "scaled_continuous_features"
  private val FEATURES = "features"
  private val PREDICTION = "prediction"
  private val LABEL_COLUMN = "item_cnt_day"

  //We read the data from the file taking into account there's a header.
  //na.drop() will return rows where all values are non-null.
  var df = spark.read
    .options(readOptions)
    .schema(StructType(Seq(
      StructField("date", StringType, nullable = true),
      StructField("date_block_num", LongType, nullable = true),
      StructField("shop_id", LongType, nullable = true),
      StructField("item_id", LongType, nullable = true),
      StructField("item_price", DoubleType, nullable = true),
      StructField("item_cnt_day", DoubleType, nullable = true))))
    .csv(salesFile)

  var items = spark.read
    .options(readOptions)
    .schema(StructType(Seq(
      StructField("item_name", StringType, nullable = true),
      StructField("item_id", LongType, nullable = true),
      StructField("item_category_id", LongType, nullable = true))))
    .csv(itemsFile)

  // Filtering and feature engineering
  df = df.filter(df.col("item_cnt_day") > 0)
    .join(items, "item_id")

  //We'll split the set into training and test data
  val Array(trainingData, testData) = df.randomSplit(Array(0.8, 0.2))

  // continuous features and categorical features
  val continuousFeatures = Array("item_price", "date_block_num")
  val categoricalFeatures = Array("shop_id", "item_id", "item_category_id")

  // Continuous features assembler and then scaler
  val continuousFeatureAssembler = new VectorAssembler()
    .setInputCols(continuousFeatures)
    .setOutputCol(UNSCALED_CONTINUOUS_FEATURES)

  val continuousFeatureScaler = new StandardScaler()
    .setInputCol(UNSCALED_CONTINUOUS_FEATURES)
    .setOutputCol(SCALED_CONTINUOUS_FEATURES)

  // Categorical features indexer
  val categoricalFeatureIndexers = categoricalFeatures.map {
    feature =>
      new StringIndexer()
        .setHandleInvalid("keep")
        .setInputCol(feature)
        .setOutputCol(s"${feature}_index")
  }

  //We define the assembler to collect the columns into a new column with a single vector - "features"
  val featureCols = categoricalFeatureIndexers.map(_.getOutputCol).union(Seq(SCALED_CONTINUOUS_FEATURES))
  val assembler = new VectorAssembler()
    .setInputCols(featureCols)
    .setOutputCol(FEATURES)

  // Model definition
  val lr = new LinearRegression()
    .setLabelCol(LABEL_COLUMN)
    .setFeaturesCol(FEATURES)
    .setPredictionCol(PREDICTION)

  //We define the Array with the stages of the pipeline
  val stages = Array(continuousFeatureAssembler, continuousFeatureScaler)
    .union(categoricalFeatureIndexers)
    .union(Seq(assembler, lr))

  //Construct the pipeline
  val pipeline = new Pipeline().setStages(stages)

  //This will evaluate the error/deviation of the regression using the Root Mean Squared deviation
  val evaluator = new RegressionEvaluator()
    .setLabelCol(LABEL_COLUMN)
    .setPredictionCol(PREDICTION)
    .setMetricName("rmse")

  // We use a ParamGridBuilder to construct a grid of parameters to search over.
  // With 3 values for hashingTF.numFeatures and 2 values for lr.regParam,
  // this grid will have 3 x 2 = 6 parameter settings for CrossValidator to choose from.
  val paramGrid = new ParamGridBuilder()
    .addGrid(lr.elasticNetParam, Array(.0, .8))
    .addGrid(lr.regParam, Array(.1, .01, .001))
    .build()

  // We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
  // This will allow us to jointly choose parameters for all Pipeline stages.
  // A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
  // Note that the evaluator here is a BinaryClassificationEvaluator and its default metric
  // is areaUnderROC.
  val cv = new CrossValidator()
    .setEstimator(pipeline)
    .setEvaluator(evaluator)
    .setEstimatorParamMaps(paramGrid)
    .setNumFolds(3) // Use 3+ in practice

  // Run cross-validation, and choose the best set of parameters.
  val cvModel = cv.fit(trainingData)

  val rmse = evaluator.evaluate(cvModel.transform(testData))

  val sbc = SparkBundleContext().withDataset(cvModel.bestModel.transform(df))
  (for (bf <- managed(BundleFile("jar:file:/tmp/spark-pipeline.zip"))) yield {
    cvModel.bestModel.writeBundle.save(bf)(sbc).get
  }).tried.get

  spark.stop()

  // print the best evaluated model RMSE
  println("Root Mean Squared Error (RMSE) on test data = " + rmse)
}
