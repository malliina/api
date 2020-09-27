package com.malliina.mavenapi

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import com.malliina.mavenapi.AppImplicits._
import com.malliina.mavenapi.AppService.pong
import io.circe.syntax._
import scalatags.Text.all._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object AppService {
  val pong = "pong"

  def apply(ctx: ContextShift[IO]): AppService = {
    val ec = ExecutionContext.global
    val http = HttpClient(ec, ctx)
    apply(http, ec, ctx)
  }

  def apply(http: HttpClient, ec: ExecutionContext, ctx: ContextShift[IO]): AppService = {
    val db = MyDatabase(ec, ctx)
    new AppService(MavenCentralClient(http), http, db)
  }
}

class AppService(maven: MavenCentralClient, http: HttpClient, data: MyDatabase) {
//  implicit def enc[T: Encoder] = jsonEncoderOf[IO, T]

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "ping" => Ok(pong)
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
        errors => BadRequest(Errors(errors.map(e => SingleError.input(e.sanitized)))),
        ok =>
          maven.search(ok).flatMap { res =>
            Ok(res)
          }
      )
    case GET -> Root / "ids" / IntVar(id)      => Ok(pong)
    case GET -> Root / "users" / UserIdVar(id) => Ok(pong)
    case GET -> Root / "hello" / name          => Ok(s"Hello, $name!")
    case GET -> Root / "html"                  => Ok(html(body(p("Hello, scalatags!!"))))
    case GET -> Root / "json"                  => Ok(Person("Michael", 17).asJson)
    case req                                   => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
  }

  object parsers {
    implicit val group = idQueryDecoder(GroupId.apply)
    implicit val artifact = idQueryDecoder(ArtifactId.apply)
    def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
      QueryParamDecoder.stringQueryParamDecoder.map(build)

    def parse[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      q.params.get(key).toRight(NonEmptyList(parseFailure(s"Query key not found: '$key'."), Nil)).flatMap { g =>
        dec.decode(QueryParameterValue(g)).toEither
      }

    def parseMavenQuery(q: Query) = for {
      g <- parse[GroupId](q, "g")
      a <- parse[ArtifactId](q, "a")
    } yield MavenQuery(g, a)
  }

  def parseFailure(message: String) = ParseFailure(message, message)
}

object AppServer extends IOApp {
  val app = Router("/" -> AppService(contextShift).service).orNotFound
  val server = BlazeServerBuilder[IO](ExecutionContext.global).bindHttp(port = 9000, "0.0.0.0").withHttpApp(app)

  override def run(args: List[String]): IO[ExitCode] = server.serve.compile.drain.as(ExitCode.Success)
}
