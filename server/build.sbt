name := "model-api"
scalaVersion := "2.11.12"

val sparkVersion = "2.3.1"
val mleapVersion = "0.10.3"
val akkaStreamVersion = "2.4.2"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

libraryDependencies ++= Seq(
  "ml.combust.mleap" %% "mleap-runtime" % mleapVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion)
