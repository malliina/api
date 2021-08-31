import com.typesafe.sbt.packager.docker.DockerVersion
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import scala.sys.process.Process
import scala.util.Try

val prodPort = 9000

val mavenapi = project
  .in(file("."))
  .enablePlugins(JavaServerAppPackaging, BuildInfoPlugin)
  .settings(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.0.1",
    libraryDependencies ++= Seq("blaze-server", "blaze-client", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.22.2"
    } ++ Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.1"
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % "1.2.5"
    } ++ Seq(
      ("com.lihaoyi" %% "scalatags" % "0.9.4").cross(CrossVersion.for3Use2_13),
      "org.slf4j" % "slf4j-api" % "1.7.32",
      "org.scalameta" %% "munit" % "0.7.28" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    dockerVersion := Option(DockerVersion(19, 3, 5, None)),
    dockerBaseImage := "openjdk:11",
    Docker / daemonUser := "mavenapi",
    Docker / version := gitHash,
    dockerRepository := Option("malliinacr.azurecr.io"),
    dockerExposedPorts ++= Seq(prodPort),
    buildInfoPackage := "com.malliina.mavenapi",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, "gitHash" -> gitHash)
  )

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")

Global / bloopExportJarClassifiers := Some(Set("sources"))
