package com.malliina.config

import com.malliina.http.FullUrl
import com.malliina.values.{ErrorMessage, Readable}
import com.typesafe.config.Config

trait ConfigReadable[T]:
  def read(key: String, c: Config): Either[ErrorMessage, T]
  def flatMap[U](f: T => ConfigReadable[U]): ConfigReadable[U] =
    val parent = this
    (key: String, c: Config) => parent.read(key, c).flatMap(t => f(t).read(key, c))
  def emap[U](f: T => Either[ErrorMessage, U]): ConfigReadable[U] = (key: String, c: Config) =>
    read(key, c).flatMap(f)
  def map[U](f: T => U): ConfigReadable[U] = emap(t => Right(f(t)))

object ConfigReadable:
  implicit val string: ConfigReadable[String] =
    recovered((key: String, c: Config) => Right(c.getString(key)))
  implicit val url: ConfigReadable[FullUrl] =
    string.emap(s => FullUrl.build(s))
  implicit val int: ConfigReadable[Int] =
    recovered((key: String, c: Config) => Right(c.getInt(key)))
  implicit val bool: ConfigReadable[Boolean] =
    recovered((key: String, c: Config) => Right(c.getBoolean(key)))
  implicit val config: ConfigReadable[Config] =
    recovered((key: String, c: Config) => Right(c.getConfig(key)))

  private def recovered[T](unsafe: ConfigReadable[T]): ConfigReadable[T] =
    (key: String, c: Config) =>
      try unsafe.read(key, c)
      catch case e => Left(ErrorMessage(Option(e.getMessage).getOrElse(s"Failed to read '$key'.")))

  implicit def readable[T](implicit r: Readable[T]): ConfigReadable[T] =
    string.emap(s => r.read(s))
