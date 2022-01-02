package com.malliina.pill.db

import cats.effect.IO
import cats.effect.kernel.Resource
import com.malliina.util.AppLogger
import com.malliina.pill.db.DoobieDatabase.log
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.DataSourceTransactor
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import scala.concurrent.duration.DurationInt

object DoobieDatabase:
  private val log = AppLogger(getClass)

  def apply(conf: DatabaseConf): Resource[IO, DatabaseRunner[IO]] =
    migratedResource(conf).map { tx => new DoobieDatabase(tx) }

  private def migratedResource(conf: DatabaseConf): Resource[IO, HikariTransactor[IO]] =
    Resource.pure(migrate(conf)).flatMap { _ => resource(hikariConf(conf)) }

  private def resource(conf: HikariConfig): Resource[IO, HikariTransactor[IO]] =
    for
      ec <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      tx <- HikariTransactor.fromHikariConfig[IO](conf, ec)
    yield tx

  private def migrate(conf: DatabaseConf): MigrateResult =
    val flyway = Flyway.configure.dataSource(conf.url, conf.user, conf.pass).load()
    flyway.migrate()

  private def hikariConf(conf: DatabaseConf): HikariConfig =
    val hikari = new HikariConfig()
    hikari.setDriverClassName(DatabaseConf.MySQLDriver)
    hikari.setJdbcUrl(conf.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass)
    hikari.setMaxLifetime(60.seconds.toMillis)
    hikari.setMaximumPoolSize(10)
    log.info(s"Connecting to '${conf.url}'...")
    hikari

class DoobieDatabase(tx: HikariTransactor[IO]) extends DatabaseRunner[IO]:
  implicit val logHandler: LogHandler = LogHandler {
    case Success(sql, args, exec, processing) =>
      log.info(s"OK '$sql' exec ${exec.toMillis} ms processing ${processing.toMillis} ms.")
    case ProcessingFailure(sql, args, exec, processing, failure) =>
      log.error(s"Failed '$sql' in ${exec + processing}.", failure)
    case ExecFailure(sql, args, exec, failure) =>
      log.error(s"Exec failed '$sql' in $exec.'", failure)
  }

  def run[T](io: ConnectionIO[T]): IO[T] = io.transact(tx)

trait DatabaseRunner[F[_]]:
  def logHandler: LogHandler
  def run[T](io: ConnectionIO[T]): F[T]
