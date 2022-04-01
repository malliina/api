scalaVersion := "2.12.15"

val utilsVersion = "1.2.13"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-bundler" % utilsVersion,
  "com.malliina" % "sbt-filetree" % "0.4.1",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6",
  "io.spray" % "sbt-revolver" % "0.9.1",
  "com.github.sbt" % "sbt-native-packager" % "1.9.7",
  "com.eed3si9n" % "sbt-buildinfo" % "0.10.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0",
  "org.scala-js" % "sbt-scalajs" % "1.9.0",
  "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.0-RC1",
  "com.eed3si9n" % "sbt-assembly" % "1.2.0"
) map addSbtPlugin
