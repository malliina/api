package com.malliina.pill

import cats.Monad
import cats.effect.*
import com.malliina.http4s.AppImplicits
import com.malliina.pill.db.{DatabaseRunner, DoobieDatabase, PillInput, PillService}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.*
import org.http4s.dsl.io.*

class PillRoutes(db: PillService[IO]) extends AppImplicits[IO]:
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      Ok(Json.obj("a" -> "b".asJson))
    case req @ GET -> Root / "what" =>
      Ok(Json.obj("c" -> "d".asJson))
    case req @ POST -> Root =>
      req.decodeJson[PillInput].flatMap { in =>
        db.save(in).flatMap { row =>
          Ok(row.asJson)
        }
      }
    case GET -> Root / "boom" =>
      throw new Exception("Kaboom!")
  }
