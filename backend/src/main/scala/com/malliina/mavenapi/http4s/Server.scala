package com.malliina.mavenapi.http4s

import cats.effect.{ExitCode, IO, IOApp}
import com.malliina.mavenapi.http4s.StaticService
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router

import scala.concurrent.ExecutionContext

object AppServer extends IOApp:
  val ec: ExecutionContext = ExecutionContext.global
  val appResource =
    for service <- Service(ec)
    yield Router("/" -> service.service, "/assets" -> StaticService[IO].routes).orNotFound
  val server = for
    app <- appResource
    server <- BlazeServerBuilder[IO](ec).bindHttp(port = 9000, "0.0.0.0").withHttpApp(app).resource
  yield server

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
