scalaVersion := "2.12.21"

val utilsVersion = "1.7.0"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "org.scalameta" % "sbt-scalafmt" % "2.6.1",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "com.eed3si9n" % "sbt-assembly" % "2.3.1",
  "com.github.sbt" % "sbt-native-packager" % "1.11.7"
) map addSbtPlugin
