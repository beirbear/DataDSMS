name := "DataDSMS"

version := "1.0"

scalaVersion := "2.12.1"

val json4sNative = "org.json4s" %% "json4s-native" % "3.5.0"
val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.5.0"
val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.1"

libraryDependencies += json4sNative
libraryDependencies += json4sJackson
libraryDependencies += akkaHttp
