package com.malliina.http4s

import com.malliina.http.Errors
import com.malliina.mavenapi.UserId
import org.http4s.*
import org.http4s.headers.`Content-Type`
import scalatags.generic.Frag
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder

trait Extractors:
  object UserIdVar:
    def unapply(str: String): Option[UserId] =
      if str.trim.nonEmpty then Option(UserId(str.trim)) else None

trait MyScalatagsInstances:
  given scalatagsEncoder[F[_], C <: Frag[?, String]](using
    charset: Charset = Charset.`UTF-8`
  ): EntityEncoder[F, C] =
    contentEncoder(MediaType.text.html)

  private def contentEncoder[F[_], C <: Frag[?, String]](mediaType: MediaType)(using
    charset: Charset
  ): EntityEncoder[F, C] =
    EntityEncoder
      .stringEncoder[F]
      .contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))

trait AppParsers[F[_]] extends BasicService[F]:
  protected def parsed[T](result: Either[Errors, T])(
    res: T => F[Response[F]]
  ): F[Response[F]] = result.fold(
    errors => badRequest(errors),
    ok => res(ok)
  )

trait AppImplicits[F[_]] extends AppParsers[F] with MyScalatagsInstances with Extractors
