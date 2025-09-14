package com.malliina.mavenapi

import cats.effect.*
import com.malliina.http.io.HttpClientIO
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.*

class TemplateTests extends munit.CatsEffectSuite:
  private val service = ResourceFunFixture(
    HttpClientIO.resource[IO].map(http => Service.default(http).service.orNotFound)
  )

  service.test("can make request"): app =>
    val pingRequest = Request[IO](Method.GET, uri"/health")
    app.run(pingRequest).map(res => assertEquals(res.status, Status.Ok))
    app.run(pingRequest).flatMap(res => res.as[AppMeta].map(_ => true).assert)

  service.test("interop with Future"): tr =>
    val request = Request[IO](Method.GET, uri"/")
    tr.run(request).map(res => assertEquals(res.status, Status.Ok))
