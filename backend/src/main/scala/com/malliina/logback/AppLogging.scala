package com.malliina.logback

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import ch.qos.logback.classic.Level
import com.malliina.http.io.HttpClientF2
import com.malliina.logstreams.client.LogstreamsUtils
import com.malliina.mavenapi.BuildInfo

object AppLogging:
  private val userAgent = s"API/${BuildInfo.version} (${BuildInfo.gitHash.take(7)})"

  def init(): Unit =
    LogbackUtils.init(levelsByLogger =
      Map("org.http4s.ember.server.EmberServerBuilderCompanionPlatform" -> Level.OFF)
    )

  def resource[F[_]: Async](d: Dispatcher[F], http: HttpClientF2[F]): Resource[F, Boolean] =
    Resource.make(LogstreamsUtils.installIfEnabled("api", userAgent, d, http))(_ =>
      Sync[F].delay(LogbackUtils.loggerContext.stop())
    )
