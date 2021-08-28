package com.malliina.mavenapi

import cats.data.NonEmptyList
import io.circe.Codec
import io.circe.generic.semiauto._

case class Name(name: String) extends AnyVal

case class UserId(id: String) extends AnyVal

case class Person(name: String, age: Int)

object Person {
  implicit val json: Codec[Person] = deriveCodec[Person]
}

case class AppResult(message: String)

object AppResult {
  val example = AppResult("The result!")
  implicit val json: Codec[AppResult] = deriveCodec[AppResult]
}

case class AppMeta(version: String, gitHash: String)

object AppMeta {
  implicit val json: Codec[AppMeta] = deriveCodec[AppMeta]
  val meta = AppMeta(BuildInfo.version, BuildInfo.gitHash)
}

case class SingleError(message: String, key: String)

object SingleError {
  implicit val json: Codec[SingleError] = deriveCodec[SingleError]
  def input(message: String): SingleError = apply(message, "input")
}

case class Errors(errors: NonEmptyList[SingleError])

object Errors {
  implicit val json: Codec[Errors] = deriveCodec[Errors]
  def apply(message: String): Errors = Errors(NonEmptyList(SingleError(message, "general"), Nil))
}
