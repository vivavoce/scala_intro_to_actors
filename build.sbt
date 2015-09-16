name := "workingActors"
 
version := "1.0"
 
scalaVersion := "2.11.7"

// scalacOptions += "-feature"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
// libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.3.11")

libraryDependencies ++= Seq(
  "com.github.wookietreiber" %% "scala-chart" % "0.4.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11"
)
