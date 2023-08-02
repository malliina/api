package com.malliina.http4s

import cats.Applicative
import com.malliina.mavenapi.Errors
import com.malliina.pill.PillRoutes.noCache
import io.circe.syntax.EncoderOps
import org.http4s.{Request, Response}

class BasicService[F[_]: Applicative] extends AppImplicits[F]:
  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverError: F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
