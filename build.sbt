import com.comcast.ip4s.port

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := versions.app,
    scalaVersion := versions.scala,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
      case PathList("META-INF", "versions", xs @ _*)            => MergeStrategy.first
      case PathList("META-INF", "okio.kotlin_module")           => MergeStrategy.first
      case PathList("com", "malliina", xs @ _*)                 => MergeStrategy.first
      case PathList("module-info.class")                        => MergeStrategy.discard
      case x                                                    =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    scalacOptions ++= Seq("-Wunused:all")
  )
)

val shared = project.in(file("shared"))

val frontend = project
  .in(file("frontend"))
  .enablePlugins(NodeJsPlugin, EsbuildPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-dom" % versions.scalaJsDom
    )
  )

val backend = project
  .in(file("backend"))
  .enablePlugins(ServerPlugin, DebPlugin)
  .settings(
    clientProject := frontend,
    dependentModule := shared,
    hashPackage := "com.malliina.mvn.assets",
    libraryDependencies ++=
      Seq("config", "logstreams-client", "database", "util-http4s", "util-html").map { m =>
        "com.malliina" %% m % versions.util
      } ++ Seq("generic", "parser").map { m =>
        "io.circe" %% s"circe-$m" % versions.circe
      } ++ Seq(
        "com.malliina" %% "mobile-push-io" % versions.mobilePush,
        "org.mariadb.jdbc" % "mariadb-java-client" % versions.mariadb,
        "com.lihaoyi" %% "scalatags" % versions.scalatags,
        "commons-codec" % "commons-codec" % versions.commonsCodec,
        "org.apache.commons" % "commons-text" % versions.commonsText,
        "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
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
