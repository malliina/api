package com.malliina.mavenapi.http4s

import cats.Applicative
import com.malliina.mavenapi.{AppImplicits, Errors}
import org.http4s.{Request, Response}
import io.circe.syntax.EncoderOps

class BasicService[F[_]] extends AppImplicits[F]:
  def notFound(req: Request[F])(implicit a: Applicative[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson)
