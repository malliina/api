package com.malliina.http4s

import cats.Applicative
import cats.data.NonEmptyList
import com.malliina.http4s.BasicService.noCache
import com.malliina.mavenapi.Errors
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.headers.`Cache-Control`
import org.http4s.{Request, Response}

object BasicService:
  val noCacheDirectives = NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
  val noCache = `Cache-Control`(noCacheDirectives)

class BasicService[F[_]: Applicative] extends AppImplicits[F]:
  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverError: F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
