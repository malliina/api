package com.malliina.http4s

import cats.effect._
import cats.implicits._
import com.malliina.http4s.AppImplicits._
import com.malliina.http4s.AppService.pong
import io.circe.syntax._
import scalatags.Text.all._
import org.http4s._
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
    new AppService(http, db)
  }
}

class AppService(http: HttpClient, data: MyDatabase) {
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
    case GET -> Root / "ids" / IntVar(id)      => Ok(pong)
    case GET -> Root / "users" / UserIdVar(id) => Ok(pong)
    case GET -> Root / "hello" / name          => Ok(s"Hello, $name!")
    case GET -> Root / "html"                  => Ok(html(body(p("Hello, scalatags!!"))))
    case GET -> Root / "json"                  => Ok(Person("Michael", 17).asJson)
    case req                                   => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
  }
}

object AppServer extends IOApp {
  val app = Router("/" -> AppService(contextShift).service).orNotFound
  val server = BlazeServerBuilder[IO].bindHttp(port = 9000, "localhost").withHttpApp(app)

  override def run(args: List[String]): IO[ExitCode] = server.serve.compile.drain.as(ExitCode.Success)
}
