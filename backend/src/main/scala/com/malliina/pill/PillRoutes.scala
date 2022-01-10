package com.malliina.pill

import cats.Monad
import cats.effect.*
import com.malliina.http4s.AppImplicits
import com.malliina.pill.PillRoutes.noCache
import com.malliina.pill.db.{DatabaseRunner, DoobieDatabase, PillService, PushToken}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.*
import org.http4s.CacheDirective.*
import org.http4s.dsl.io.*
import org.http4s.headers.{Accept, Location, `Cache-Control`, `WWW-Authenticate`}

object PillRoutes:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

class PillRoutes(db: PillService[IO]) extends AppImplicits[IO]:
  implicit def jsonResponses[A: Encoder]: EntityEncoder[IO, A] =
    jsonEncoderOf[IO, A]

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
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
