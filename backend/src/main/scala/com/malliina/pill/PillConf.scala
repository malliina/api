package com.malliina.pill

import com.malliina.config.ConfigReadable
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.Paths

object PillConf:
  val appDir = Paths.get(sys.props("user.home")).resolve(".pill")
  val localConfFile = appDir.resolve("pill.conf")
  val localConfig = ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())

  def picsConf = ConfigFactory.load(localConfig).resolve().getConfig("pill")

  implicit class ConfigOps(c: Config) extends AnyVal:
    def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
      r.read(key, c)
    def unsafe[T: ConfigReadable](key: String): T =
      c.read[T](key).fold(err => throw new IllegalArgumentException(err.message), identity)
