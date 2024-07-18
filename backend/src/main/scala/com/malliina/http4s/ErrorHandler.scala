package com.malliina.http4s

import cats.effect.Async
import com.malliina.http.ResponseException
import com.malliina.http4s.ErrorHandler.log
import com.malliina.util.AppLogger
import org.http4s.{Request, Response}

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

class ErrorHandler[F[_]: Async] extends BasicApiService[F] with AppImplicits[F]:
  def handler: Request[F] => PartialFunction[Throwable, F[Response[F]]] =
    req => partial

  def partial: PartialFunction[Throwable, F[Response[F]]] =
    case re: ResponseException =>
      val error = re.error
      log.error(s"HTTP ${error.code} for '${error.url}'. Body: '${error.response.asString}'.")
      serverError
    case NonFatal(t) =>
      log.error(s"Server error.", t)
      serverError
