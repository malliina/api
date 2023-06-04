package com.malliina.pill

import com.malliina.config.{ConfigException, ConfigReadable}
import com.malliina.mavenapi.BuildInfo
import com.malliina.pill.db.DatabaseConf
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.{Path, Paths}

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

case class PillConf(mode: AppMode, db: DatabaseConf, apnsPrivateKey: Path):
  def apnsEnabled = apnsPrivateKey.toString != "changeme"

object PillConf:
  val appDir = Paths.get(sys.props("user.home")).resolve(".pill")
  val localConfFile = appDir.resolve("pill.conf")
  val localConfig = ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  implicit val pathConfig: ConfigReadable[Path] = ConfigReadable.string.map { s =>
    Paths.get(s)
  }
  private def pillConf =
    val conf =
      if BuildInfo.isProd then ConfigFactory.load("application-prod.conf")
      else ConfigFactory.load(localConfig)
    conf.resolve().getConfig("pill")

  implicit class ConfigOps(c: Config) extends AnyVal:
    def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
      r.read(key, c)
    def unsafe[T: ConfigReadable](key: String): T =
      c.read[T](key).fold(err => throw IllegalArgumentException(err.message), identity)

  def unsafe(c: Config = pillConf): PillConf =
    apply(c).fold(err => throw ConfigException(err), identity)

  def apply(c: Config): Either[ErrorMessage, PillConf] =
    for
      mode <- c.read[AppMode]("mode")
      db <- c.read[DatabaseConf]("db")
      apnsPrivateKey <- c.read[Path]("push.apns.privateKey")
    yield PillConf(mode, db, apnsPrivateKey)
