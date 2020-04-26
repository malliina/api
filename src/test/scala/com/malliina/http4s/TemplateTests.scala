package com.malliina.http4s

import cats.effect._
import munit.FunSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class TemplateTests extends FunSuite {
  implicit val ec = ExecutionContext.global
  implicit val ctx = IO.contextShift(ec)
  val service = AppService(ctx).service.orNotFound

  test("can make request") {
    val pingRequest = Request[IO](Method.GET, uri"/ping")
    val response = service.run(pingRequest).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
    val body = response.as[String].unsafeRunSync()
    assertEquals(body, AppService.pong)
  }

  test("interop with Future") {
    implicit val dec = jsonOf[IO, AppResult]
    val request = Request[IO](Method.GET, uri"/items")
    val response = service.run(request).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
    val result = response.as[AppResult].unsafeRunSync()
    assertEquals(result, AppResult.example)
  }
}
