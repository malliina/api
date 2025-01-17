package com.malliina.pill

import com.malliina.config.ConfigReadable.ConfigOps
import com.malliina.config.{ConfigError, ConfigReadable}
import com.malliina.database.Conf
import com.malliina.mavenapi.BuildInfo
import com.malliina.values.{AccessToken, Password}
import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.{Path, Paths}
import com.malliina.http.UrlSyntax.url

case class PillConf(
  isTest: Boolean,
  isProdBuild: Boolean,
  db: Conf,
  apnsPrivateKey: Path,
  discoToken: AccessToken
):
  def apnsEnabled = apnsPrivateKey.toString != "changeme"
  def isFull = isProdBuild || isTest

object PillConf:
  private val appDir = Paths.get(sys.props("user.home")).resolve(".pill")
  private val localConfFile = appDir.resolve("pill.conf")
  private val localConfig =
    ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  given ConfigReadable[Path] = ConfigReadable.string.map(s => Paths.get(s))

  private def pillConf =
    val conf =
      if BuildInfo.isProd then ConfigFactory.load("application-prod.conf")
      else ConfigFactory.load(localConfig)
    conf.resolve().getConfig("pill")

  def apply(c: Config = pillConf): Either[ConfigError, PillConf] =
    val isProd = BuildInfo.isProd
    for
      dbPass <- c.parse[Password]("db.pass")
      apnsPrivateKey <- c.parse[Path]("push.apns.privateKey")
      discoToken <- c.parse[AccessToken]("discogs.token")
    yield PillConf(
      isTest = false,
      isProdBuild = isProd,
      if isProd then prodDatabaseConf(dbPass) else devDatabaseConf(dbPass),
      apnsPrivateKey,
      discoToken
    )

  private def prodDatabaseConf(password: Password) = Conf(
    url"jdbc:mysql://localhost:3306/pill",
    "pill",
    password,
    Conf.MySQLDriver,
    2,
    autoMigrate = true
  )

  private def devDatabaseConf(password: Password) = Conf(
    url"jdbc:mysql://localhost:3307/pill",
    "pill",
    password,
    Conf.MySQLDriver,
    2,
    autoMigrate = false
  )
