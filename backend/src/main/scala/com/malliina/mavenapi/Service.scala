package com.malliina.mavenapi

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.toFlatMapOps
import com.malliina.http.io.HttpClientF2
import com.malliina.http.{Errors, SingleError}
import com.malliina.http4s.{AppImplicits, FormDecoders, parsers}
import com.malliina.mavenapi.html.Pages
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.{HttpRoutes, Request, Response, Uri}

object Service:
  val pong = "pong"

  def default[F[_]: {Async, Parallel}](http: HttpClientF2[F]): Service[F] =
    val db = MyDatabase[F]
    Service[F](MavenCentralClient[F](http), db)

class Service[F[_]: {Async, Parallel}](maven: MavenCentralClient[F], data: MyDatabase[F])
  extends AppImplicits[F]
  with FormDecoders[F]:
  private val pages = Pages.default()

  private def parseMaven(req: Request[F])(run: MavenQuery => F[Response[F]]): F[Response[F]] =
    val result = parsers
      .parseMavenQuery(req.uri.query)
      .left
      .map: errors =>
        Errors(errors.map(e => SingleError.input(e.sanitized)))
    parsed(result): q =>
      run(q)

  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      parseMaven(req): q =>
        if q.isEmpty then ok(pages.search(q, Nil))
        else
          maven
            .search(q)
            .flatMap: res =>
              ok(pages.search(q, res.results))
    case GET -> Root / "db" =>
      data.load.flatMap(res => Ok(res.message))
    case req @ POST -> Root =>
      req.decode[SearchForm]: form =>
        val baseUri: Uri = uri"/"
        val dest = baseUri.withQueryParams(form.nonEmpty.toMap)
        seeOther(dest)
    case GET -> Root / "health"          => Ok(AppMeta.meta.asJson)
    case req @ GET -> Root / "artifacts" =>
      parseMaven(req): q =>
        maven.search(q).flatMap(res => ok(res))
    case req => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
