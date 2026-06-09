package com.malliina.http4s

import cats.effect.Async
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.util.AppLogger
import io.circe.{Codec, Json}
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes

case class KeyValue(key: String, value: String) derives Codec.AsObject

class LoggerService[F[_]: Async] extends BasicService[F]:
  private val log = AppLogger(getClass)
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req =>
      val headers = req.headers.headers.map: raw =>
        KeyValue(raw.name.toString, raw.value)
      req.asJson
        .flatMap: json =>
          log.info(
            s"Got ${req.method} ${req.uri}: '${json.noSpaces}' headers ${headers.asJson.noSpaces}."
          )
          ok(
            Json.obj(
              "method" -> req.method.name.asJson,
              "uri" -> req.uri.renderString.asJson,
              "body" -> json,
              "headers" -> headers.asJson
            )
          )
        .handleErrorWith: t =>
          log.info(s"Got ${req.method} ${req.uri} headers ${headers.asJson.noSpaces}.", t)
          ok(
            Json.obj(
              "method" -> req.method.name.asJson,
              "uri" -> req.uri.renderString.asJson,
              "headers" -> headers.asJson
            )
          )
