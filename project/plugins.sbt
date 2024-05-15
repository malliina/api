scalaVersion := "2.12.19"

val utilsVersion = "1.6.36"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.eed3si9n" % "sbt-assembly" % "2.2.0",
  "com.github.sbt" % "sbt-native-packager" % "1.10.0"
) map addSbtPlugin
