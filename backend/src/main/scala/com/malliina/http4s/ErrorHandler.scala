package com.malliina.http4s

import cats.Monad
import cats.effect.IO
import com.malliina.http4s.AppImplicits.*
import com.malliina.mavenapi.Errors
import com.malliina.util.AppLogger
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.headers.{Connection, `Content-Length`}
import org.http4s.{Headers, Request, Response, Status}
import org.typelevel.ci.CIStringSyntax

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

  def handler: Request[IO] => PartialFunction[Throwable, IO[Response[IO]]] =
    req => { case NonFatal(t) =>
      log.error(s"Server error: ${req.method} ${req.pathInfo}. Exception $t", t)
      InternalServerError(Errors("Server error."))
    }
