name := "SudoBot"

version := "1.0"

//lazy val `tradehubbot` = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(cache, ws, specs2 % Test,
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.json4s" %% "json4s-ext" % "3.3.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.slick" %% "slick-codegen" % "3.1.1",
    "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "co.theasi" %% "plotly" % "0.2.0",
  "org.apache.commons" % "commons-email" % "1.4",
  "org.apache.poi" % "poi" % "3.15",
  "org.apache.poi" % "poi-ooxml" % "3.15",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.15",
  "org.scala-lang" % "scala-library" % "2.11.7"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers ++= Seq("scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Typesafe private" at "https://private-repo.typesafe.com/typesafe/maven-releases")



lazy val commonSettings = Seq(
  organization := "com.dn",
  scalaVersion := "2.11.5"
)

lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
lazy val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
lazy val dispatchV = "0.11.2" // change this to appropriate dispatch version
lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV
