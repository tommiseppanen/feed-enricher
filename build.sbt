javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    name := "feed-enricher",
    version := "1.0",
    scalaVersion := "2.11.8",
    retrieveManaged := true,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.0.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.0.0",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.0",
    libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.0.0"
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

    