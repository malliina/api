package com.malliina.pill

import cats.effect.*
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http4s.AppImplicits
import com.malliina.http4s.BasicApiService.noCache
import com.malliina.pill.db.PillService
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.*
import org.http4s.headers.`Cache-Control`

class PillRoutes[F[_]: Concurrent](db: PillService[F]) extends AppImplicits[F]:
  given jsonResponses[A: Encoder]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root =>
      Ok(Json.obj("a" -> "b".asJson))
    case GET -> Root / "what" =>
      Ok(Json.obj("c" -> "d".asJson))
    case req @ POST -> Root / "enable" =>
      for
        body <- req.decodeJson[EnablePillNotifications]
        row <- db.enable(body)
        res <- Ok(row, noCache)
      yield res
    case req @ POST -> Root / "disable" =>
      for
        body <- req.decodeJson[DisablePillNotifications]
        _ <- db.disable(body)
        res <- Ok(PillResponse.done, noCache)
      yield res
    case GET -> Root / "boom" =>
      throw new Exception("Kaboom!")
