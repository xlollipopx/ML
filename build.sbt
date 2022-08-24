ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10"

val sparkVersion    = "3.2.1"
val vegasVersion    = "0.3.11"
val postgresVersion = "42.2.2"
val circeV          = "0.12.3"

lazy val root = (project in file("."))
  .settings(
    name := "Analyzer"
  )

val AkkaVersion     = "2.6.8"
val AkkaHttpVersion = "10.2.9"

resolvers ++= Seq(
  "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
  "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
  "MavenRepository" at "https://mvnrepository.com"
)

libraryDependencies ++= Seq(
  "org.apache.spark"        %% "spark-core"       % sparkVersion,
  "org.apache.spark"        %% "spark-sql"        % sparkVersion,
  "org.apache.spark"        %% "spark-mllib"      % sparkVersion,
  "org.apache.logging.log4j" % "log4j-api"        % "2.4.1",
  "org.apache.logging.log4j" % "log4j-core"       % "2.4.1",
  "org.postgresql"           % "postgresql"       % postgresVersion,
  "com.typesafe"             % "config"           % "1.3.3",
  "com.typesafe.akka"       %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka"       %% "akka-stream"      % AkkaVersion,
  "com.typesafe.akka"       %% "akka-http"        % AkkaHttpVersion,
  "io.circe"                %% "circe-core"       % circeV,
  "io.circe"                %% "circe-generic"    % circeV,
  "io.circe"                %% "circe-parser"     % circeV,
  // Sugar for serialization and deserialization in akka-http with circe
  "de.heikoseeberger" %% "akka-http-circe" % "1.36.0"
)
