package com.malliina.logback

import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.toFlatMapOps
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.core.ConsoleAppender
import com.malliina.http.HttpClient
import com.malliina.http.io.HttpClientF2
import com.malliina.logback.fs2.FS2AppenderComps
import com.malliina.logstreams.client.FS2Appender
import com.malliina.mavenapi.BuildInfo
import com.malliina.util.AppLogger
import org.slf4j.LoggerFactory

object LogstreamsUtils:
  def prepLogging(): Unit =
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    lc.reset()
    val ple = PatternLayoutEncoder()
    ple.setPattern("""%d{HH:mm:ss.SSS} %-5level %logger{72} %msg%n""")
    ple.setContext(lc)
    ple.start()
    val console = new ConsoleAppender[ILoggingEvent]()
    console.setEncoder(ple)
    LogbackUtils.installAppender(console)
    val root = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    root.setLevel(Level.INFO)
    lc.getLogger("org.http4s.ember.server.EmberServerBuilderCompanionPlatform").setLevel(Level.OFF)

  def install[F[_]: Async](d: Dispatcher[F], http: HttpClientF2[F]): F[Unit] =
    val enabled = sys.env.get("LOGSTREAMS_ENABLED").contains("true")
    if enabled then
      FS2Appender
        .default(
          d,
          http,
          Map("User-Agent" -> s"API/${BuildInfo.version} (${BuildInfo.gitHash.take(7)})")
        )
        .flatMap { appender =>
          Async[F].delay {
            appender.setName("LOGSTREAMS")
            appender.setEndpoint("wss://logs.malliina.com/ws/sources")
            appender.setUsername(sys.env.getOrElse("LOGSTREAMS_USER", "api"))
            appender.setPassword(sys.env.getOrElse("LOGSTREAMS_PASS", ""))
            appender.setEnabled(enabled)
            LogbackUtils.installAppender(appender)
          }
        }
    else Async[F].unit
