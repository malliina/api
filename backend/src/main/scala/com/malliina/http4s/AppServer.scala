package com.malliina.http4s

import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.{Monad, Parallel}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.database.DoobieDatabase
import com.malliina.http.io.HttpClientIO
import com.malliina.logback.AppLogging
import com.malliina.mavenapi.Service
import com.malliina.pill.db.PillService
import com.malliina.pill.{PillConf, PillRoutes, Push, PushService}
import com.malliina.util.AppLogger
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.{Router, Server}
import org.http4s.{Http, HttpRoutes, Request, Response}

import scala.concurrent.duration.{Duration, DurationInt}

object AppServer extends IOApp:
  override def runtimeConfig =
    super.runtimeConfig.copy(cpuStarvationCheckInitialDelay = Duration.Inf)
  AppLogging.init()
  private val log = AppLogger(getClass)
  log.info("Starting server...")
  private val serverPort: Port =
    sys.env.get("SERVER_PORT").flatMap(s => Port.fromString(s)).getOrElse(port"9000")
  private def appResource[F[+_]: Async: Parallel]: Resource[F, Http[F, F]] =
    for
      http <- HttpClientIO.resource[F]
      dispatcher <- Dispatcher.parallel[F]
      _ <- AppLogging.resource(dispatcher, http)
      service = Service.default[F](http)
      conf <- Resource.eval(
        PillConf.apply().fold(err => Async[F].raiseError(err), c => Async[F].pure(c))
      )
      push =
        if conf.apnsEnabled then Push.default[F](conf.apnsPrivateKey, http) else PushService.noop[F]
      db <- DoobieDatabase.default(conf.db)
      pill = PillRoutes(PillService(db))
    yield GZip {
      HSTS {
        orNotFound {
          Router(
            "/" -> service.service,
            "/pill" -> pill.service,
            "/assets" -> StaticService[F].routes
          )
        }
      }
    }
  private def emberServer[F[+_]: Async: Parallel]: Resource[F, Server] = for
    app <- appResource
    server <- EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(serverPort)
      .withHttpApp(app)
      .withIdleTimeout(60.seconds)
      .withRequestHeaderReceiveTimeout(30.seconds)
      .withErrorHandler(ErrorHandler[F].partial)
      .withShutdownTimeout(1.millis)
      .build
  yield server

  def orNotFound[F[_]: Monad](rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    Kleisli(req => rs.run(req).getOrElseF(BasicService[F].notFound(req)))

  override def run(args: List[String]): IO[ExitCode] =
    emberServer[IO].use(_ => IO.never).as(ExitCode.Success)
