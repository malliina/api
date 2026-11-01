package com.malliina.musicmeta

import com.malliina.http.FullUrl
import com.malliina.values.{ErrorMessage, StringEnumCompanion}
import io.circe.Codec

enum ResultType(val name: String):
  case Master extends ResultType("master")
  case Release extends ResultType("release")
  case Other(n: String) extends ResultType(n)

object ResultType extends StringEnumCompanion[ResultType]:
  override def all: Seq[ResultType] = Seq(Master, Release)
  override def write(t: ResultType): String = t.name

  override def build(input: String): Either[ErrorMessage, ResultType] =
    Right(all.find(i => write(i).toLowerCase == input.toLowerCase).getOrElse(Other(input)))

case class CoverResult(cover_image: FullUrl, `type`: ResultType) derives Codec.AsObject:
  def tpe = `type`

case class CoverSearchResult(results: Seq[CoverResult]) derives Codec.AsObject
