package com.malliina.mavenapi.http4s

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import com.malliina.mavenapi.AppImplicits.*
import com.malliina.mavenapi.html.Pages
import com.malliina.mavenapi.http4s.AppServer.ec
import com.malliina.mavenapi.http4s.Service.pong
import com.malliina.mavenapi.{MavenQuery, html as _, *}
import io.circe.syntax.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.Uri
import org.http4s.server.Router
import org.http4s.{HttpRoutes, UrlForm}
import org.http4s.headers.{Accept, Location, `Cache-Control`, `WWW-Authenticate`}
import org.slf4j.LoggerFactory
import scalatags.Text.all.*

import scala.concurrent.ExecutionContext

object Service:
  val pong = "pong"

  def apply(ec: ExecutionContext): Resource[IO, Service] =
    for http <- HttpClient(ec)
    yield apply(http, ec)

  def apply(http: HttpClient, ec: ExecutionContext): Service =
    val db = MyDatabase(ec)
    new Service(MavenCentralClient(http), http, db)

class Service(maven: MavenCentralClient, http: HttpClient, data: MyDatabase):
  private val log = LoggerFactory.getLogger(getClass)

  val pages = Pages()
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      val e = parsers.parseMavenQuery(req.uri.query)
      e.fold(
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized))).asJson),
        ok =>
          if ok.isEmpty then Ok(pages.search(ok, Nil))
          else
            maven.search(ok).flatMap { res =>
              Ok(pages.search(ok, res.results))
            }
      )

    case req @ POST -> Root =>
      req.decode[UrlForm] { form =>
        val a = form.getFirst("artifact").filter(_.trim.nonEmpty).map(ArtifactId.apply)
        val g = form.getFirst("group").filter(_.trim.nonEmpty).map(GroupId.apply)
        val sv = form.getFirst("scala").filter(_.trim.nonEmpty).map(ScalaVersion.apply)
        val baseUri: Uri = uri"/"
        val map = a.map(a => Map(ArtifactId.key -> a.id)).getOrElse(Map.empty) ++ g
          .map(g => Map(GroupId.key -> g.id))
          .getOrElse(Map.empty) ++ sv.map(v => Map(ScalaVersion.key -> v.id)).getOrElse(Map.empty)
        val dest = baseUri.withQueryParams(map)
        SeeOther(Location(dest))
      }
    case GET -> Root / "health" => Ok(AppMeta.meta.asJson)
    case req @ GET -> Root / "artifacts" =>
      val e = parsers.parseMavenQuery(req.uri.query)
      e.fold(
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized))).asJson),
        ok =>
          maven.search(ok).flatMap { res =>
            Ok(res.asJson)
          }
      )
    case req => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
  }
  import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}

  object parsers:
    implicit val group: QueryParamDecoder[GroupId] = idQueryDecoder(GroupId.apply)
    implicit val artifact: QueryParamDecoder[ArtifactId] = idQueryDecoder(ArtifactId.apply)
    implicit val sv: QueryParamDecoder[ScalaVersion] = idQueryDecoder(ScalaVersion.apply)

    def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
      QueryParamDecoder.stringQueryParamDecoder.map(build)

    def parseOrDefault[T](q: Query, key: String, default: => T)(implicit
      dec: QueryParamDecoder[T]
    ) =
      parseOpt[T](q, key).getOrElse(Right(default))

    def parse[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      parseOpt[T](q, key).getOrElse(
        Left(NonEmptyList(parseFailure(s"Query key not found: '$key'."), Nil))
      )

    def parseOpt[T](q: Query, key: String)(implicit
      dec: QueryParamDecoder[T]
    ): Option[Either[NonEmptyList[ParseFailure], T]] =
      q.params.get(key).map { g =>
        dec.decode(QueryParameterValue(g)).toEither
      }

    def parseOpt2[T](q: Query, key: String)(implicit
      dec: QueryParamDecoder[T]
    ): Either[NonEmptyList[ParseFailure], Option[T]] =
      q.params
        .get(key)
        .map { g =>
          dec.decode(QueryParameterValue(g)).toEither.map(Option.apply)
        }
        .getOrElse {
          Right(None)
        }

    def parseMavenQuery(q: Query): Either[NonEmptyList[ParseFailure], MavenQuery] = for
      g <- parseOpt2[GroupId](q, GroupId.key)
      a <- parseOpt2[ArtifactId](q, ArtifactId.key)
      sv <- parseOrDefault[ScalaVersion](q, ScalaVersion.key, ScalaVersion.scala213)
    yield MavenQuery(g, a, sv)

  def parseFailure(message: String) = ParseFailure(message, message)
