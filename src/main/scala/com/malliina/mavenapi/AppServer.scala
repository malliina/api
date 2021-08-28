package com.malliina.mavenapi

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import com.malliina.mavenapi.AppImplicits.*
import com.malliina.mavenapi.AppServer.{ec}
import com.malliina.mavenapi.AppService.pong
import io.circe.syntax.*
import org.http4s.HttpRoutes
import scalatags.Text.all.*
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object AppService {
  val pong = "pong"

  def apply(ctx: ContextShift[IO], ec: ExecutionContext): Resource[IO, AppService] =
    for {
      http <- HttpClient(ec)(ctx)
    } yield apply(http, ec, ctx)

  def apply(http: HttpClient, ec: ExecutionContext, ctx: ContextShift[IO]): AppService = {
    val db = MyDatabase(ec, ctx)
    new AppService(MavenCentralClient(http), http, db)
  }
}

class AppService(maven: MavenCentralClient, http: HttpClient, data: MyDatabase) {
//  implicit def enc[T: Encoder] = jsonEncoderOf[IO, T]
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "ping"   => Ok(pong)
    case GET -> Root / "health" => Ok(AppMeta.meta.asJson)
    case GET -> Root / "items" =>
      data.load.flatMap { items =>
        Ok(items.asJson)
      }
    case GET -> Root / "proxy" =>
      http.demo.flatMap { s =>
        Ok(s)
      }
    case req @ GET -> Root / "artifacts" =>
      val e = parsers.parseMavenQuery(req.uri.query)
      e.fold(
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized))).asJson),
        ok =>
          maven.search(ok).flatMap { res =>
            Ok(res.asJson)
          }
      )
    case GET -> Root / "ids" / IntVar(id)      => Ok(pong)
    case GET -> Root / "users" / UserIdVar(id) => Ok(pong)
    case GET -> Root / "hello" / name          => Ok(s"Hello, $name!")
    case GET -> Root / "html"                  => Ok(html(body(p("Hello, scalatags!!"))))
    case GET -> Root / "json"                  => Ok(Person("Michael", 17).asJson)
    case req                                   => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
  }
  import org.http4s.{Query, QueryParamDecoder, ParseFailure, QueryParameterValue}

  object parsers {
    implicit val group: QueryParamDecoder[GroupId] = idQueryDecoder(GroupId.apply)
    implicit val artifact: QueryParamDecoder[ArtifactId] = idQueryDecoder(ArtifactId.apply)
    implicit val sv: QueryParamDecoder[ScalaVersion] = idQueryDecoder(ScalaVersion.apply)

    def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
      QueryParamDecoder.stringQueryParamDecoder.map(build)

    def parseOrDefault[T](q: Query, key: String, default: => T)(implicit dec: QueryParamDecoder[T]) =
      parseOpt[T](q, key).getOrElse(Right(default))

    def parse[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      parseOpt[T](q, key).getOrElse(Left(NonEmptyList(parseFailure(s"Query key not found: '$key'."), Nil)))

    def parseOpt[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      q.params.get(key).map { g =>
        dec.decode(QueryParameterValue(g)).toEither
      }

    def parseMavenQuery(q: Query) = for {
      g <- parse[GroupId](q, "g")
      a <- parse[ArtifactId](q, "a")
      sv <- parseOrDefault[ScalaVersion](q, "sv", ScalaVersion.scala213)
    } yield MavenQuery(g, a, sv)
  }

  def parseFailure(message: String) = ParseFailure(message, message)
}

object AppServer extends IOApp {
  val ec: ExecutionContext = ExecutionContext.global
  val appResource = for {
    service <- AppService(contextShift, ec)
  } yield Router("/" -> service.service).orNotFound
  val server = for {
    app <- appResource
    server <- BlazeServerBuilder[IO](ec).bindHttp(port = 9000, "0.0.0.0").withHttpApp(app).resource
  } yield server

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
