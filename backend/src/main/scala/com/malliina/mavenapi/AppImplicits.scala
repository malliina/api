package com.malliina.mavenapi

import _root_.scalatags.generic.Frag
import cats.effect.IO
import org.http4s.*
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

trait Extractors:
  object UserIdVar:
    def unapply(str: String): Option[UserId] =
      if str.trim.nonEmpty then Option(UserId(str.trim)) else None

trait MyScalatagsInstances:
  implicit def scalatagsEncoder[F[_], C <: Frag[?, String]](implicit
    charset: Charset = DefaultCharset
  ): EntityEncoder[F, C] =
    contentEncoder(MediaType.text.html)

  private def contentEncoder[F[_], C <: Frag[?, String]](mediaType: MediaType)(implicit
    charset: Charset
  ): EntityEncoder[F, C] =
    EntityEncoder
      .stringEncoder[F]
      .contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))

trait AppImplicits[F[_]]
  extends syntax.AllSyntax
  with Http4sDsl[F]
  with CirceInstances
  with MyScalatagsInstances
  with Extractors

object AppImplicits extends AppImplicits[IO]
