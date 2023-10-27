package com.malliina.mavenapi

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.toFlatMapOps
import com.malliina.http.io.HttpClientF2
import com.malliina.http4s.{AppImplicits, parsers}
import com.malliina.mavenapi.html.Pages
import com.malliina.mavenapi.{html as _, *}
import io.circe.syntax.EncoderOps
import org.http4s.headers.Location
import org.http4s.{HttpRoutes, Uri, UrlForm}

object Service:
  val pong = "pong"

  def default[F[_]: Async: Parallel](http: HttpClientF2[F]): Service[F] =
    val db = MyDatabase[F]
    Service[F](MavenCentralClient[F](http), db)

class Service[F[_]: Async: Parallel](maven: MavenCentralClient[F], data: MyDatabase[F])
  extends AppImplicits[F]:
  private val pages = Pages.default()
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      val e = parsers.parseMavenQuery(req.uri.query)
      e.fold(
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized))).asJson),
        ok =>
          if ok.isEmpty then Ok(pages.search(ok, Nil))
          else
            maven
              .search(ok)
              .flatMap: res =>
                Ok(pages.search(ok, res.results))
      )
    case GET -> Root / "db" =>
      data.load.flatMap(res => Ok(res.message))
    case req @ POST -> Root =>
      req.decode[UrlForm]: form =>
        val a = form.getFirst("artifact").filter(_.trim.nonEmpty).map(ArtifactId.apply)
        val g = form.getFirst("group").filter(_.trim.nonEmpty).map(GroupId.apply)
        val sv = form.getFirst("scala").filter(_.trim.nonEmpty).map(ScalaVersion.apply)
        val baseUri: Uri = uri"/"
        val map = a.map(a => Map(ArtifactId.key -> a.id)).getOrElse(Map.empty) ++ g
          .map(g => Map(GroupId.key -> g.id))
          .getOrElse(Map.empty) ++ sv.map(v => Map(ScalaVersion.key -> v.id)).getOrElse(Map.empty)
        val dest = baseUri.withQueryParams(map)
        SeeOther(Location(dest))
    case GET -> Root / "health" => Ok(AppMeta.meta.asJson)
    case req @ GET -> Root / "artifacts" =>
      val e = parsers.parseMavenQuery(req.uri.query)
      e.fold(
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized))).asJson),
        ok => maven.search(ok).flatMap(res => Ok(res.asJson))
      )
    case req => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
