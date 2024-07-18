package com.malliina.mavenapi

import cats.effect.*
import com.malliina.http.io.HttpClientIO
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.*

class TemplateTests extends munit.CatsEffectSuite:
  private val serviceFixture = ResourceFunFixture(
    HttpClientIO.resource[IO].map(http => Service.default(http).service.orNotFound)
  )

  serviceFixture.test("can make request"): tr =>
    val pingRequest = Request[IO](Method.GET, uri"/health")
    tr.run(pingRequest).map(res => assertEquals(res.status, Status.Ok))
    tr.run(pingRequest).flatMap(res => res.as[AppMeta].map(_ => true).assert)

  serviceFixture.test("interop with Future"): tr =>
//    implicit val dec: EntityDecoder[IO, AppResult] = jsonOf[IO, AppResult]
    val request = Request[IO](Method.GET, uri"/")
    tr.run(request).map(res => assertEquals(res.status, Status.Ok))
