package com.malliina.http4s

import cats.Applicative
import cats.effect.IO
import com.malliina.mavenapi.Errors
import com.malliina.pill.PillRoutes.noCache
import org.http4s.{Request, Response}
import io.circe.syntax.EncoderOps

object BasicService extends BasicService[IO]

class BasicService[F[_]] extends AppImplicits[F]:
  def notFound(req: Request[F])(implicit a: Applicative[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverError(implicit a: Applicative[F]): F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
