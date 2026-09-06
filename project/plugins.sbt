val utilsVersion = "2.0.3"

Seq(
  "com.malliina" % "sbt-nodejs" % utilsVersion,
  "com.malliina" % "sbt-revolver-rollup" % utilsVersion,
  "com.malliina" % "sbt-filetree" % utilsVersion,
  "org.scalameta" % "sbt-scalafmt" % "2.6.1",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0",
  "com.eed3si9n" % "sbt-assembly" % "2.5.0",
  "com.github.sbt" % "sbt-native-packager" % "1.11.7"
) map addSbtPlugin
