package com.malliina.mavenapi

import com.malliina.http4s.FormReadableT
import com.malliina.values.Literals.err
import com.malliina.values.{ErrorMessage, Readable}
import io.circe.{Codec, Decoder, Encoder}

trait IdComp[T <: String]:
  def build(s: String): Either[ErrorMessage, T] =
    if s.isBlank then Left(err"Input was blank.")
    else Right(apply(s))

  protected def apply(s: String): T
  extension (t: T)
    def id: String = t
    def trimmed: T = apply(id.trim)
    def nonEmpty: Boolean = id.nonEmpty

  given Readable[T] = Readable.string.map(s => apply(s))
  given Codec[T] = Codec.from(
    Decoder.decodeString.map(apply),
    Encoder.encodeString.contramap(_.id)
  )

opaque type ArtifactId = String

object ArtifactId extends IdComp[ArtifactId]:
  val key = "a"
  protected def apply(s: String): ArtifactId = s.trim

opaque type GroupId = String

object GroupId extends IdComp[GroupId]:
  val key = "g"
  protected def apply(s: String): GroupId = s.trim

opaque type Version = String

object Version extends IdComp[Version]:
  protected def apply(s: String): Version = s.trim

opaque type ScalaVersion = String

object ScalaVersion extends IdComp[ScalaVersion]:
  def apply(s: String): ScalaVersion = s.trim
  val scala213 = apply("2.13")
  val scala3 = apply("3")
  val sjs1scala213 = apply("sjs1_2.13")
  val sjs1scala3 = apply("sjs1_3")
  val sbt1 = apply("2.12_1.0")
  val key = "sv"

case class SearchForm(a: Option[ArtifactId], g: Option[GroupId], sv: Option[ScalaVersion]):
  def nonEmpty: SearchForm =
    SearchForm(
      a.filter(_.trimmed.nonEmpty),
      g.filter(_.trimmed.nonEmpty),
      sv.filter(_.trimmed.nonEmpty)
    )

  def toMap = a.map(a => Map(ArtifactId.key -> a.id)).getOrElse(Map.empty) ++ g
    .map(g => Map(GroupId.key -> g.id))
    .getOrElse(Map.empty) ++ sv.map(v => Map(ScalaVersion.key -> v.id)).getOrElse(Map.empty)

object SearchForm:
  given FormReadableT[SearchForm] = FormReadableT.reader.emap: reader =>
    for
      a <- reader.read[Option[ArtifactId]]("artifact")
      g <- reader.read[Option[GroupId]]("group")
      sv <- reader.read[Option[ScalaVersion]]("scala")
    yield SearchForm(a, g, sv)

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

case class SearchResult(id: String, g: GroupId, a: String, v: String, timestamp: Long)
  derives Codec.AsObject:
  private val isScala = a.contains('_')
  private val artifactName = a.takeWhile(_ != '_')
  private val sep = if isScala then "%%" else "%"
  val sbt = s""""$g" $sep "$artifactName" % "$v""""

case class MavenResponse(docs: Seq[SearchResult], start: Int, numFound: Int) derives Codec.AsObject

case class MavenSearchResponse(response: MavenResponse) derives Codec.AsObject

case class SearchResults(results: Seq[SearchResult]) derives Codec.AsObject
