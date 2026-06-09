package com.malliina.http4s

import cats.effect.Async
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.util.AppLogger
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes

class LoggerService[F[_]: Async] extends BasicService[F]:
  private val log = AppLogger(getClass)
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req =>
      req.asJson
        .flatMap: json =>
          log.info(s"Got ${req.method} ${req.uri}: '${json.noSpaces}'.")
          ok(
            Json.obj(
              "method" -> req.method.name.asJson,
              "uri" -> req.uri.renderString.asJson,
              "body" -> json
            )
          )
        .handleErrorWith: t =>
          log.info(s"Got ${req.method} ${req.uri}.", t)
          ok(Json.obj("method" -> req.method.name.asJson, "uri" -> req.uri.renderString.asJson))
