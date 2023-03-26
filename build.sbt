import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import com.comcast.ip4s.IpLiteralSyntax

val munitVersion = "0.7.29"

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.2.2",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
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

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.4.0"
    )
  )

val backend = project
  .in(file("backend"))
  .enablePlugins(
    FileTreePlugin,
    ServerPlugin,
    LiveRevolverPlugin
  )
  .settings(
    clientProject := frontend,
    hashPackage := "com.malliina.mvn.assets",
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.18"
    } ++ Seq("core", "hikari").map { m =>
      "org.tpolecat" %% s"doobie-$m" % "1.0.0-RC2"
    } ++ Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.5"
    } ++ Seq(
      "com.typesafe" % "config" % "1.4.2",
      "mysql" % "mysql-connector-java" % "8.0.32",
      "org.flywaydb" % "flyway-core" % "7.15.0",
      "com.malliina" %% "mobile-push-io" % "3.7.1",
      "com.lihaoyi" %% "scalatags" % "0.12.0",
      "com.malliina" %% "logstreams-client" % "2.5.0",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    buildInfoPackage := "com.malliina.mavenapi",
    buildInfoKeys ++= Seq[BuildInfoKey](
      name,
      version,
      scalaVersion
    ),
    assembly / assemblyJarName := "app.jar",
    liveReloadPort := port"10102"
  )

val api = project
  .in(file("."))
  .aggregate(frontend, backend)

Global / onChangedBuildSource := ReloadOnSourceChanges
