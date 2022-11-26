package com.malliina.http4s

import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.http.io.HttpClientIO
import com.malliina.http4s.{BasicService, StaticService}
import com.malliina.mavenapi.Service
import com.malliina.pill.db.{DoobieDatabase, PillService}
import com.malliina.pill.{PillConf, PillRoutes, Push, PushService}
import com.malliina.util.AppLogger
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.{Http, HttpApp, HttpRoutes, Request, Response}
import com.malliina.storage.StorageLong
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import cats.effect.IO.asyncForIO

object AppServer extends IOApp:
  private val log = AppLogger(getClass)
  private val serverPort: Port =
    sys.env.get("SERVER_PORT").flatMap(s => Port.fromString(s)).getOrElse(port"9000")
  private val appResource: Resource[IO, Http[IO, IO]] =
    for
      http <- HttpClientIO.resource
      service = Service.default(http)
      conf = PillConf.unsafe()
      push = if conf.apnsEnabled then Push(conf.apnsPrivateKey, http) else PushService.noop[IO]
      db <- DoobieDatabase(conf.db)
      pill = PillRoutes(PillService(db))
    yield GZip {
      HSTS {
        orNotFound {
          Router(
            "/" -> service.service,
            "/pill" -> pill.service,
            "/assets" -> StaticService[IO].routes
          )
        }
      }
    }
  val emberServer: Resource[IO, Server] = for
    app <- appResource
    server <- EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(serverPort)
      .withHttpApp(app)
      .withIdleTimeout(60.seconds)
      .withRequestHeaderReceiveTimeout(30.seconds)
      .withErrorHandler(ErrorHandler[IO].partial)
      .withShutdownTimeout(1.millis)
      .build
  yield server

  def orNotFound(rs: HttpRoutes[IO]): Kleisli[IO, Request[IO], Response[IO]] =
    Kleisli(req => rs.run(req).getOrElseF(BasicService.notFound(req)))

  override def run(args: List[String]): IO[ExitCode] =
    emberServer.use(_ => IO.never).as(ExitCode.Success)
