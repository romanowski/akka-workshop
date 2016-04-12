name := "workshop-server"

organization := "com.virtuslab"

version := "1.0.1"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-remote_2.11" % "2.4.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.3",
  "com.virtuslab" %% "akka-workshop-distributor" % "1.0.1",
  "com.virtuslab" %% "akka-workshop-decrypter" % "1.0.1"
)
