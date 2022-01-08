import com.typesafe.sbt.packager.docker.DockerVersion
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import scala.sys.process.Process
import scala.util.Try

val prodPort = 9000

val munitVersion = "0.7.29"

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.1.0"
  )
)

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, ClientPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    assetsPackage := "com.malliina.mvn.assets",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.1.0",
      "org.scalameta" %%% "munit" % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    webpack / version := "5.65.0",
    webpackCliVersion := "4.9.1",
    startWebpackDevServer / version := "4.5.0",
    webpackEmitSourceMaps := false,
    scalaJSUseMainModuleInitializer := true,
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    Compile / npmDependencies ++= Seq(
      "@fortawesome/fontawesome-free" -> "5.15.4",
      "@popperjs/core" -> "2.11.0",
      "bootstrap" -> "5.1.3"
    ),
    Compile / npmDevDependencies ++= Seq(
      "autoprefixer" -> "10.4.1",
      "cssnano" -> "5.0.14",
      "css-loader" -> "6.5.1",
      "less" -> "4.1.2",
      "less-loader" -> "10.2.0",
      "mini-css-extract-plugin" -> "2.4.5",
      "postcss" -> "8.4.5",
      "postcss-import" -> "14.0.2",
      "postcss-loader" -> "6.2.1",
      "postcss-preset-env" -> "7.2.0",
      "style-loader" -> "3.3.1",
      "webpack-merge" -> "5.8.0"
    ),
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.dev.config.js"),
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.prod.config.js"),
    Compile / fastOptJS / webpackBundlingMode := BundlingMode.LibraryOnly(),
    Compile / fullOptJS / webpackBundlingMode := BundlingMode.Application
  )

val backend = project
  .in(file("backend"))
  .enablePlugins(
    FileTreePlugin,
    JavaServerAppPackaging,
    BuildInfoPlugin,
    ServerPlugin,
    LiveRevolverPlugin
  )
  .settings(
    clientProject := frontend,
    libraryDependencies ++= Seq("blaze-server", "blaze-client", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.7"
    } ++ Seq("doobie-core", "doobie-hikari").map { d =>
      "org.tpolecat" %% d % "1.0.0-RC1"
    } ++ Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.1"
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % "1.2.10"
    } ++ Seq(
      "com.typesafe" % "config" % "1.4.1",
      "mysql" % "mysql-connector-java" % "5.1.49",
      "org.flywaydb" % "flyway-core" % "7.15.0",
      "com.malliina" %% "mobile-push-io" % "3.1.0",
      "com.lihaoyi" %% "scalatags" % "0.11.0",
      "org.slf4j" % "slf4j-api" % "1.7.32",
      "com.malliina" %% "logstreams-client" % "2.0.2",
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    dockerVersion := Option(DockerVersion(19, 3, 5, None)),
    dockerBaseImage := "openjdk:11",
    Docker / daemonUser := "mavenapi",
    Docker / version := gitHash,
    Compile / doc / sources := Seq.empty,
    Docker / packageName := "mavenapi",
    dockerRepository := Option("malliinacr.azurecr.io"),
    dockerExposedPorts ++= Seq(prodPort),
    buildInfoPackage := "com.malliina.mavenapi",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, "gitHash" -> gitHash),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "public",
    Universal / javaOptions ++= {
      Seq(
        "-J-Xmx1024m",
        "-Dlogback.configurationFile=logback-prod.xml"
      )
    }
  )

val api = project
  .in(file("."))
  .aggregate(frontend, backend)
  .settings(
    start := (backend / start).value
  )

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")

Global / onChangedBuildSource := ReloadOnSourceChanges
