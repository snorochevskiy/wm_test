enablePlugins(JavaServerAppPackaging)

import Dependencies._

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "snorochevskiy"
ThisBuild / organizationName := "wmtest"

lazy val root = (project in file("."))
  .settings(
    name := "wm_test",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10",
      "com.typesafe.akka" %% "akka-stream" % "2.6.10",
      "com.typesafe.akka" %% "akka-http" % "10.2.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "com.iheart" %% "ficus" % "1.5.0",

      scalaTest % Test,
      "org.awaitility" % "awaitility" % "4.0.3" % Test
    )
  )

mainClass in Compile := Some("snorochevskiy.wm.Main")