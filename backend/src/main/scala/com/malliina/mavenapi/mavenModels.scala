package com.malliina.mavenapi

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*

case class ArtifactId(id: String) extends AnyVal with PrimitiveId
object ArtifactId extends PrimitiveCompanion[ArtifactId]:
  val key = "a"
case class GroupId(id: String) extends AnyVal with PrimitiveId
object GroupId extends PrimitiveCompanion[GroupId]:
  val key = "g"
case class Version(id: String) extends AnyVal with PrimitiveId
object Version extends PrimitiveCompanion[Version]
case class ScalaVersion(id: String) extends AnyVal with PrimitiveId
object ScalaVersion extends PrimitiveCompanion[ScalaVersion]:
  val scala213 = apply("2.13")
  val scala3 = apply("3")
  val sjs1scala213 = apply("sjs1_2.13")
  val sjs1scala3 = apply("sjs1_3")
  val key = "sv"

case class MavenQuery(
  group: Option[GroupId],
  artifact: Option[ArtifactId],
  scalaVersion: ScalaVersion
):
  def scalaArtifactName = artifact.map(a => s"${a}_$scalaVersion")
  def isEmpty = group.isEmpty && artifact.isEmpty
  def describe = (group, artifact) match
    case (Some(g), Some(a)) => s"artifact $a in $g"
    case (Some(g), None)    => s"group $g"
    case (None, Some(a))    => s"artifact $a"
    case (None, None)       => "no query"

trait PrimitiveId extends Any:
  def id: String
  override def toString = id

trait PrimitiveCompanion[T <: PrimitiveId]:
  def apply(s: String): T
  implicit val dec: Decoder[T] = Decoder.decodeString.map[T](s => apply(s))
  implicit val enc: Encoder[T] = Encoder.encodeString.contramap[T](_.id)

case class MavenDocument(id: String, g: GroupId, a: String, v: String, timestamp: Long)

object MavenDocument:
  implicit val json: Codec[MavenDocument] = deriveCodec[MavenDocument]

case class MavenResponse(docs: Seq[MavenDocument], start: Int, numFound: Int)

object MavenResponse:
  implicit val json: Codec[MavenResponse] = deriveCodec[MavenResponse]

case class MavenSearchResponse(response: MavenResponse)

object MavenSearchResponse:
  implicit val json: Codec[MavenSearchResponse] = deriveCodec[MavenSearchResponse]

case class MavenSearchResults(results: Seq[MavenDocument])

object MavenSearchResults:
  implicit val json: Codec[MavenSearchResults] = deriveCodec[MavenSearchResults]
