import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import scala.sys.process.Process
import scala.util.Try
import com.comcast.ip4s.IpLiteralSyntax

val munitVersion = "0.7.29"

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.2.1",
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
      case PathList("META-INF", "versions", xs @ _*) => MergeStrategy.first
      case PathList("com", "malliina", xs @ _*) => MergeStrategy.first
      case PathList("module-info.class") => MergeStrategy.discard
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
      "org.scala-js" %%% "scalajs-dom" % "2.4.0",
      "org.scalameta" %%% "munit" % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / npmDependencies ++= Seq(
      "@popperjs/core" -> "2.11.6",
      "bootstrap" -> "5.2.3"
    ),
    Compile / npmDevDependencies ++= Seq(
      "autoprefixer" -> "10.4.13",
      "cssnano" -> "5.1.15",
      "css-loader" -> "6.7.3",
      "less" -> "4.1.3",
      "less-loader" -> "11.1.0",
      "mini-css-extract-plugin" -> "2.7.2",
      "postcss" -> "8.4.21",
      "postcss-import" -> "15.1.0",
      "postcss-loader" -> "7.0.2",
      "postcss-preset-env" -> "8.0.1",
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
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.18"
    } ++ Seq("core", "hikari").map { m =>
      "org.tpolecat" %% s"doobie-$m" % "1.0.0-RC2"
    } ++ Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.3"
    } ++ Seq(
      "com.typesafe" % "config" % "1.4.2",
      "mysql" % "mysql-connector-java" % "8.0.32",
      "org.flywaydb" % "flyway-core" % "7.15.0",
      "com.malliina" %% "mobile-push-io" % "3.7.1",
      "com.lihaoyi" %% "scalatags" % "0.12.0",
      "com.malliina" %% "logstreams-client" % "2.5.0",
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
      "publicDir" -> (Compile / resourceDirectory).value.toPath.resolve("public"),
      "publicFolder" -> (frontend / assetsPrefix).value,
      "mode" -> (if ((frontend / isProd).value) "prod" else "dev"),
      "isProd" -> (frontend / isProd).value
    ),
    (frontend / Compile / start) := Def.taskIf {
      if ((frontend / Compile / start).inputFileChanges.hasChanges) {
        refreshBrowsers.value
      } else {
        Def.task(streams.value.log.info("No frontend changes.")).value
      }
    }.dependsOn(frontend / Compile / start).value,
    Compile / unmanagedResourceDirectories ++= {
      val prodAssets =
        if ((frontend / isProd).value) List((frontend / Compile / assetsRoot).value.getParent.toFile)
        else Nil
      (baseDirectory.value / "public") +: prodAssets
    },
    assembly / assemblyJarName := "app.jar",
    liveReloadPort := port"10102"
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
