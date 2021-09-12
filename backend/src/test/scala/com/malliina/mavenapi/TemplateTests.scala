package com.malliina.mavenapi

import com.malliina.mavenapi.http4s.Service
import cats.effect.*
import munit.FunSuite
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import io.circe.Decoder
import scala.concurrent.ExecutionContext
import cats.effect.unsafe.implicits.global

class TemplateTests extends FunSuite:
  implicit val ec: ExecutionContext = ExecutionContext.global
  val serviceFixture = resourceFixture(Service(ec).map(_.service.orNotFound))

  serviceFixture.test("can make request") { tr =>
    val pingRequest = Request[IO](Method.GET, uri"/ping")
    val response = tr.resource.run(pingRequest).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
    val body = response.as[String].unsafeRunSync()
    assertEquals(body, Service.pong)
  }

  serviceFixture.test("interop with Future") { tr =>
    implicit val dec: EntityDecoder[IO, AppResult] = jsonOf[IO, AppResult]
    val request = Request[IO](Method.GET, uri"/items")
    val response = tr.resource.run(request).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
    val result = response.as[AppResult].unsafeRunSync()
    assertEquals(result, AppResult.example)
  }

  case class TestResource[T](resource: T, close: IO[Unit])

  def resourceFixture[T](res: Resource[IO, T]) = FunFixture[TestResource[T]](
    setup = options =>
      val (t, finalizer) = res.allocated.unsafeRunSync()
      TestResource(t, finalizer),
    teardown = tr => tr.close.unsafeRunSync()
  )
