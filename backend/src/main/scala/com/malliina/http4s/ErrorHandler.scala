package com.malliina.http4s

import cats.Monad
import cats.effect.{Async, IO}
import com.malliina.http.ResponseException
import com.malliina.http4s.AppImplicits.*
import com.malliina.http4s.ErrorHandler.log
import com.malliina.mavenapi.Errors
import com.malliina.pill.PillRoutes.noCache
import com.malliina.util.AppLogger
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.headers.{Connection, `Content-Length`}
import org.http4s.{Headers, Request, Response, Status}
import org.typelevel.ci.CIStringSyntax

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

class ErrorHandler[F[_]: Async] extends BasicService[F]:
  def handler: Request[F] => PartialFunction[Throwable, F[Response[F]]] =
    req => { case NonFatal(t) =>
      log.error(s"Server errors: ${req.method} ${req.pathInfo}. Exception $t", t)
      InternalServerError(Errors("Server error."), noCache)
    }

  def partial: PartialFunction[Throwable, F[Response[F]]] =
    case re: ResponseException =>
      val error = re.error
      log.error(s"HTTP ${error.code} for '${error.url}'. Body: '${error.response.asString}'.")
      serverError
    case NonFatal(t) =>
      log.error(s"Server error.", t)
      serverError
