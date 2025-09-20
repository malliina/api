package com.malliina.mavenapi

import cats.effect.*
import com.malliina.http.HttpHeaders
import com.malliina.http.UrlSyntax.https
import com.malliina.http.io.HttpClientIO
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.*

class TemplateTests extends munit.CatsEffectSuite:
  val http = ResourceFunFixture(HttpClientIO.resource[IO])
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

  http.test("hmm".ignore): client =>
    val url =
      https"central.sonatype.com/api/internal/browse/component/versions?sortField=normalizedVersion&sortDirection=desc&page=0&size=12&filter=namespace%3Acom.malliina%2Cname%3Amobile-push_3"
    client
      .get(url, Map("Accept" -> HttpHeaders.application.json))
      .map: res =>
        assertEquals(res.status, 200)
        println(res.asString)
