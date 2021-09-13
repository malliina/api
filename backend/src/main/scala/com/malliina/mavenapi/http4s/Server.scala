package com.malliina.mavenapi.http4s

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.malliina.mavenapi.http4s.StaticService
import com.malliina.mavenapi.http4s.BasicService
import org.http4s.{Request, Response}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.{HttpRoutes, Request, Response}

import scala.concurrent.ExecutionContext

object AppServer extends IOApp:
  val ec: ExecutionContext = ExecutionContext.global
  val appResource =
    for service <- Service(ec)
    yield GZip {
      HSTS {
        orNotFound {
          Router("/" -> service.service, "/assets" -> StaticService[IO].routes)
        }
      }
    }
  val server = for
    app <- appResource
    server <- BlazeServerBuilder[IO](ec).bindHttp(port = 9000, "0.0.0.0").withHttpApp(app).resource
  yield server

  def orNotFound(rs: HttpRoutes[IO]): Kleisli[IO, Request[IO], Response[IO]] =
    Kleisli(req => rs.run(req).getOrElseF(BasicService.notFound(req)))

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
