name := """play-scala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-remote_2.11" % "2.3.4",
  jdbc,
  anorm,
  cache,
  ws
)
