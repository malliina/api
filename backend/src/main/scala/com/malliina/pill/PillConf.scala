package com.malliina.pill

import com.malliina.config.{ConfigError, ConfigReadable}
import com.malliina.config.ConfigReadable.ConfigOps
import com.malliina.mavenapi.BuildInfo
import com.malliina.database.Conf
import com.malliina.values.{AccessToken, ErrorMessage}
import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.{Path, Paths}

sealed trait AppMode:
  def isProd = this == AppMode.Prod

object AppMode:
  case object Prod extends AppMode
  case object Dev extends AppMode

  implicit val reader: ConfigReadable[AppMode] = ConfigReadable.string.emapParsed {
    case "prod" => Right(Prod)
    case "dev"  => Right(Dev)
    case other  => Left(ErrorMessage("Must be 'prod' or 'dev'."))
  }

case class PillConf(mode: AppMode, db: Conf, apnsPrivateKey: Path, discoToken: AccessToken):
  def apnsEnabled = apnsPrivateKey.toString != "changeme"

object PillConf:
  private val appDir = Paths.get(sys.props("user.home")).resolve(".pill")
  private val localConfFile = appDir.resolve("pill.conf")
  private val localConfig =
    ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  implicit val pathConfig: ConfigReadable[Path] = ConfigReadable.string.map { s =>
    Paths.get(s)
  }
  private def pillConf =
    val conf =
      if BuildInfo.isProd then ConfigFactory.load("application-prod.conf")
      else ConfigFactory.load(localConfig)
    conf.resolve().getConfig("pill")

  def apply(c: Config = pillConf): Either[ConfigError, PillConf] =
    for
      mode <- c.parse[AppMode]("mode")
      db <- c.parse[Conf]("db")
      apnsPrivateKey <- c.parse[Path]("push.apns.privateKey")
      discoToken <- c.parse[AccessToken]("discogs.token")
    yield PillConf(mode, db, apnsPrivateKey, discoToken)
