package com.malliina.mavenapi

import com.malliina.http.FullUrl
import fs2.io.net.SocketTimeoutException
import io.circe.Codec

case class Name(name: String) extends AnyVal

case class UserId(id: String) extends AnyVal

case class Person(name: String, age: Int) derives Codec.AsObject

case class AppResult(message: String) derives Codec.AsObject

object AppResult:
  val example = AppResult("The result!")

case class AppMeta(version: String, gitHash: String) derives Codec.AsObject

object AppMeta:
  val meta = AppMeta(BuildInfo.version, BuildInfo.gitHash)

class TimeoutException(val url: FullUrl, val inner: SocketTimeoutException) extends Exception(inner)
