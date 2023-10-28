scalaVersion := "2.12.18"

val utilsVersion = "1.6.27"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "io.spray" % "sbt-revolver" % "0.10.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "org.scala-js" % "sbt-scalajs" % "1.14.0",
  "com.eed3si9n" % "sbt-assembly" % "2.1.4"
) map addSbtPlugin
