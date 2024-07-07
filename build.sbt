import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import com.comcast.ip4s.IpLiteralSyntax

val munitVersion = "1.0.0"

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.4.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
      case PathList("META-INF", "versions", xs @ _*)            => MergeStrategy.first
      case PathList("META-INF", "okio.kotlin_module")           => MergeStrategy.first
      case PathList("com", "malliina", xs @ _*)                 => MergeStrategy.first
      case PathList("module-info.class")                        => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    scalacOptions ++= Seq("-Wunused:all")
  )
)

val shared = project.in(file("shared"))

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, RollupPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0"
    )
  )

val backend = project
  .in(file("backend"))
  .enablePlugins(ServerPlugin, DebPlugin)
  .settings(
    clientProject := frontend,
    dependentModule := shared,
    hashPackage := "com.malliina.mvn.assets",
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.27"
    } ++ Seq("generic", "parser").map(m => "io.circe" %% s"circe-$m" % "0.14.9") ++ Seq(
      "com.malliina" %% "mobile-push-io" % "3.11.0",
      "com.malliina" %% "config" % "3.7.1",
      "com.malliina" %% "logstreams-client" % "2.8.0",
      "com.malliina" %% "database" % "6.8.0",
      "mysql" % "mysql-connector-java" % "8.0.33",
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "commons-codec" % "commons-codec" % "1.17.0",
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
    ),
    buildInfoPackage := "com.malliina.mavenapi",
    buildInfoKeys ++= Seq[BuildInfoKey](name, version, scalaVersion),
    assembly / assemblyJarName := "app.jar",
    liveReloadPort := port"10102",
    Compile / resourceDirectories += io.Path.userHome / ".pill",
    Linux / name := "api"
  )

val api = project
  .in(file("."))
  .aggregate(frontend, backend)

Global / onChangedBuildSource := ReloadOnSourceChanges
