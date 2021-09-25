package com.malliina.pill

import com.malliina.config.{ConfigException, ConfigReadable}
import com.malliina.pill.db.DatabaseConf
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.Paths

sealed trait AppMode:
  def isProd = this == AppMode.Prod

object AppMode:
  case object Prod extends AppMode
  case object Dev extends AppMode

  implicit val reader: ConfigReadable[AppMode] = ConfigReadable.string.emap {
    case "prod" => Right(Prod)
    case "dev"  => Right(Dev)
    case other  => Left(ErrorMessage("Must be 'prod' or 'dev'."))
  }

case class PillConf(mode: AppMode, db: DatabaseConf)

object PillConf:
  val appDir = Paths.get(sys.props("user.home")).resolve(".pill")
  val localConfFile = appDir.resolve("pill.conf")
  val localConfig = ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  implicit val databaseConfig: ConfigReadable[DatabaseConf] = ConfigReadable.config.emap { c =>
    for
      url <- c.read[String]("url")
      user <- c.read[String]("user")
      pass <- c.read[String]("pass")
    yield DatabaseConf(url, user, pass)
  }
  private def pillConf = ConfigFactory.load(localConfig).resolve().getConfig("pill")

  implicit class ConfigOps(c: Config) extends AnyVal:
    def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
      r.read(key, c)
    def unsafe[T: ConfigReadable](key: String): T =
      c.read[T](key).fold(err => throw new IllegalArgumentException(err.message), identity)

  def unsafe(c: Config = pillConf): PillConf =
    apply(c).fold(err => throw new ConfigException(err), identity)

  def apply(c: Config): Either[ErrorMessage, PillConf] =
    for
      mode <- c.read[AppMode]("mode")
      db <- c.read[DatabaseConf]("db")
    yield PillConf(mode, db)
