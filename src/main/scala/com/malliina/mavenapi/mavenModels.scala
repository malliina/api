package com.malliina.mavenapi

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class ArtifactId(id: String) extends AnyVal with PrimitiveId
object ArtifactId extends PrimitiveCompanion[ArtifactId]
case class GroupId(id: String) extends AnyVal with PrimitiveId
object GroupId extends PrimitiveCompanion[GroupId]
case class Version(id: String) extends AnyVal with PrimitiveId
object Version extends PrimitiveCompanion[Version]

case class MavenQuery(group: GroupId, artifact: ArtifactId)

trait PrimitiveId extends Any {
  def id: String
  override def toString = id
}

trait PrimitiveCompanion[T <: PrimitiveId] {
  def apply(s: String): T
  implicit val dec = Decoder.decodeString.map[T](s => apply(s))
  implicit val enc = Encoder.encodeString.contramap[T](_.id)
}

case class MavenDocument(id: String, g: GroupId, a: String, v: String, timestamp: Long)

object MavenDocument {
  implicit val json = deriveCodec[MavenDocument]
}

case class MavenResponse(docs: Seq[MavenDocument], start: Int, numFound: Int)

object MavenResponse {
  implicit val json = deriveCodec[MavenResponse]
}

case class MavenSearchResponse(response: MavenResponse)

object MavenSearchResponse {
  implicit val json = deriveCodec[MavenSearchResponse]
}

case class MavenSearchResults(results: Seq[MavenDocument])

object MavenSearchResults {
  implicit val json = deriveCodec[MavenSearchResults]
}
