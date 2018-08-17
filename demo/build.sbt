name := "model-training"
scalaVersion := "2.11.12"

val sparkVersion = "2.3.1"
val mleapVersion = "0.10.3"
val akkaStreamVersion = "2.4.2"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-catalyst" % sparkVersion,
  "ml.combust.mleap" %% "mleap-spark" % mleapVersion)
