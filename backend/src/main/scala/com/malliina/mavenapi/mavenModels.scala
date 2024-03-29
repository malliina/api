package com.malliina.mavenapi

import io.circe.{Codec, Decoder, Encoder}
import com.malliina.values.{WrappedString, StringCompanion}

case class ArtifactId(id: String) extends AnyVal with WrappedString:
  def value: String = id
object ArtifactId extends StringCompanion[ArtifactId]:
  val key = "a"

case class GroupId(id: String) extends AnyVal with WrappedString:
  def value: String = id
object GroupId extends StringCompanion[GroupId]:
  val key = "g"

case class Version(id: String) extends AnyVal with WrappedString:
  def value: String = id
object Version extends StringCompanion[Version]

case class ScalaVersion(id: String) extends AnyVal with WrappedString:
  def value: String = id
object ScalaVersion extends StringCompanion[ScalaVersion]:
  val scala213 = apply("2.13")
  val scala3 = apply("3")
  val sjs1scala213 = apply("sjs1_2.13")
  val sjs1scala3 = apply("sjs1_3")
  val sbt1 = apply("2.12_1.0")
  val key = "sv"

case class MavenQuery(
  group: Option[GroupId],
  artifact: Option[ArtifactId],
  scalaVersion: Option[ScalaVersion]
):
  def artifactName = scalaArtifactName.orElse(artifact.map(_.id))
  private def scalaArtifactName = for
    a <- artifact
    sv <- scalaVersion
  yield s"${a}_$sv"
  def isEmpty = group.isEmpty && artifact.isEmpty
  def describe = (group, artifact) match
    case (Some(g), Some(a)) => s"artifact $a in $g"
    case (Some(g), None)    => s"group $g"
    case (None, Some(a))    => s"artifact $a"
    case (None, None)       => "no query"

case class MavenDocument(id: String, g: GroupId, a: String, v: String, timestamp: Long)
  derives Codec.AsObject:
  private val isScala = a.contains('_')
  private val artifactName = a.takeWhile(_ != '_')
  private val sep = if isScala then "%%" else "%"
  val sbt = s""""$g" $sep "$artifactName" % "$v""""

case class MavenResponse(docs: Seq[MavenDocument], start: Int, numFound: Int) derives Codec.AsObject

case class MavenSearchResponse(response: MavenResponse) derives Codec.AsObject

case class MavenSearchResults(results: Seq[MavenDocument]) derives Codec.AsObject
