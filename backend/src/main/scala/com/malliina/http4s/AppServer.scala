package com.malliina.http4s

import cats.data.Kleisli
import cats.effect.kernel.{Resource, Sync}
import cats.effect.std.Dispatcher
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.{Monad, Parallel}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.database.DoobieDatabase
import com.malliina.http.io.HttpClientIO
import com.malliina.logback.AppLogging
import com.malliina.mavenapi.Service
import com.malliina.musicmeta.{CoverService, DiscoClient}
import com.malliina.pill.db.PillService
import com.malliina.pill.{PillConf, PillRoutes, Push, PushService}
import com.malliina.util.{AppLogger, Sys}
import com.malliina.values.{ErrorMessage, Readable}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.{Router, Server}
import org.http4s.{Http, HttpRoutes, Request, Response}

import scala.concurrent.duration.{Duration, DurationInt}

trait ServerResources:
  given Readable[Port] =
    Readable.string.emap(s => Port.fromString(s).toRight(ErrorMessage(s"Not a port: '$s'.")))

  private val serverPort: Port =
    Sys.env.readOpt[Port]("SERVER_PORT").getOrElse(port"9000")

  private def appResource[F[+_]: Async: Parallel](conf: PillConf): Resource[F, Http[F, F]] =
    for
      http <- HttpClientIO.resource[F]
      dispatcher <- Dispatcher.parallel[F]
      _ <- AppLogging.resource(dispatcher, http)
      maven = Service.default[F](http)
      disco = CoverService(DiscoClient(conf.discoToken, http))
      push =
        if conf.apnsEnabled then Push.default[F](conf.apnsPrivateKey, http) else PushService.noop[F]
      db <-
        if conf.isFull then DoobieDatabase.init(conf.db)
        else Resource.eval(DoobieDatabase.fast(conf.db))
      pill = PillRoutes(PillService(db))
    yield GZip:
      HSTS:
        orNotFound:
          Router(
            "/" -> maven.service,
            "/covers" -> disco.service,
            "/pill" -> pill.service,
            "/assets" -> StaticService[F].routes
          )

  def emberServer[F[+_]: Async: Parallel](conf: PillConf): Resource[F, Server] = for
    app <- appResource(conf)
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

  private def orNotFound[F[_]: Monad](rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    Kleisli(req => rs.run(req).getOrElseF(BasicApiService[F].notFound(req)))

object AppServer extends IOApp with ServerResources:
  override def runtimeConfig =
    super.runtimeConfig.copy(cpuStarvationCheckInitialDelay = Duration.Inf)
  AppLogging.init()
  private val log = AppLogger(getClass)
  log.info("Starting server...")

  override def run(args: List[String]): IO[ExitCode] =
    val server = for
      conf <- Resource.eval(Sync[IO].fromEither(PillConf.prod))
      server <- emberServer[IO](conf)
    yield server
    server.use(_ => IO.never).as(ExitCode.Success)
