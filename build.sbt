import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import scala.sys.process.Process
import scala.util.Try

val munitVersion = "0.7.29"

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.1.1",
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
      case PathList("META-INF", "versions", xs @ _*) => MergeStrategy.rename
      case PathList("com", "malliina", xs @ _*)         => MergeStrategy.first
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
)

val isProd = settingKey[Boolean]("isProd")

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
    Compile / npmDependencies ++= Seq(
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
    isProd := (Global / scalaJSStage).value == FullOptStage
  )

val backend = project
  .in(file("backend"))
  .enablePlugins(
    FileTreePlugin,
    BuildInfoPlugin,
    ServerPlugin,
    LiveRevolverPlugin
  )
  .settings(
    clientProject := frontend,
    libraryDependencies ++= Seq("ember-server", "blaze-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.11"
    } ++ Seq("core", "hikari").map { m =>
      "org.tpolecat" %% s"doobie-$m" % "1.0.0-RC2"
    } ++ Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.1"
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % "1.2.11"
    } ++ Seq(
      "com.typesafe" % "config" % "1.4.2",
      "mysql" % "mysql-connector-java" % "5.1.49",
      "org.flywaydb" % "flyway-core" % "7.15.0",
      "com.malliina" %% "mobile-push-io" % "3.4.2",
      "com.lihaoyi" %% "scalatags" % "0.11.1",
      "org.slf4j" % "slf4j-api" % "1.7.36",
      "com.malliina" %% "logstreams-client" % "2.1.6",
      "org.scalameta" %% "munit" % munitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoPackage := "com.malliina.mavenapi",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      "gitHash" -> gitHash,
      "assetsDir" -> (frontend / assetsRoot).value,
      "publicFolder" -> (frontend / assetsPrefix).value,
      "mode" -> (if ((frontend / isProd).value) "prod" else "dev"),
      "isProd" -> (frontend / isProd).value
    ),
    start := Def.taskIf {
      if (start.inputFileChanges.hasChanges) {
        refreshBrowsers.value
      } else {
        Def.task(streams.value.log.info("No backend changes."))
      }
    }.dependsOn(start).value,
    (frontend / start) := Def.taskIf {
      if ((frontend / start).inputFileChanges.hasChanges) {
        refreshBrowsers.value
      } else {
        Def.task(streams.value.log.info("No frontend changes.")).value
      }
    }.dependsOn(frontend / start).value,
    Compile / unmanagedResourceDirectories ++= {
      val prodAssets =
        if ((frontend / isProd).value) List((frontend / Compile / assetsRoot).value.getParent.toFile)
        else Nil
      (baseDirectory.value / "public") +: prodAssets
    },
    assembly / assemblyJarName := "app.jar",
    liveReloadPort := 10102
  )

val api = project
  .in(file("."))
  .aggregate(frontend, backend)

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")

Global / onChangedBuildSource := ReloadOnSourceChanges
