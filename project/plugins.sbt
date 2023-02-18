scalaVersion := "2.12.17"

val utilsVersion = "1.4.0"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-bundler" % utilsVersion,
  "com.malliina" % "sbt-filetree" % "0.4.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0",
  "io.spray" % "sbt-revolver" % "0.9.1",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0",
  "org.scala-js" % "sbt-scalajs" % "1.13.0",
  "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1",
  "com.eed3si9n" % "sbt-assembly" % "2.1.1"
) map addSbtPlugin
