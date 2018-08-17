name := "mleap-demo"
scalaVersion := "2.11.12"

import sbtassembly.MergeStrategy

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case _                             => MergeStrategy.first
  })

lazy val `demo` = project.in(file("demo"))
  .settings(
    Common.settings,
    assemblySettings,
    libraryDependencies ++= Dependencies.trainingDependencies
  )

lazy val `server` = project.in(file("server"))
  .settings(
    Common.settings,
    assemblySettings,
    libraryDependencies ++= Dependencies.serverDependencies
  )
