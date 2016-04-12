name := "akka-workshop-distributor"

organization := "com.virtuslab"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

version := "1.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.9",
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

//  realm=Artifactory Realm
//  host=localhost
//  user=xxx
//  password=xxx

credentials += Credentials(Path.userHome / ".ivy2" / ".local-credentials")

publishTo := Some("Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local")