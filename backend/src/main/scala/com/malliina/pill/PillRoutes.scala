package com.malliina.pill

import cats.Monad
import cats.effect.*
import cats.syntax.all.*
import com.malliina.http4s.AppImplicits
import com.malliina.pill.PillRoutes.noCache
import com.malliina.pill.db.PillService
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.*
import org.http4s.CacheDirective.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Cache-Control`

object PillRoutes:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

class PillRoutes[F[_]: Monad: Concurrent](db: PillService[F]) extends AppImplicits[F]:
  implicit def jsonResponses[A: Encoder]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  val service: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      Ok(Json.obj("a" -> "b".asJson))
    case req @ GET -> Root / "what" =>
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
        task <- db.disable(body)
        res <- Ok(PillResponse("Done."), noCache)
      yield res
    case GET -> Root / "boom" =>
      throw new Exception("Kaboom!")
  }
