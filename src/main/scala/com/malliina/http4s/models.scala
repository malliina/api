package com.malliina.http4s

import io.circe.generic.semiauto._

case class Name(name: String) extends AnyVal

case class UserId(id: String) extends AnyVal

case class Person(name: String, age: Int)

object Person {
  implicit val json = deriveCodec[Person]
}

case class AppResult(message: String)

object AppResult {
  val example = AppResult("The result!")
  implicit val json = deriveCodec[AppResult]
}

case class Error(message: String)

object Error {
  implicit val json = deriveCodec[Error]
}

case class Errors(errors: Seq[Error])

object Errors {
  implicit val json = deriveCodec[Errors]
  def apply(message: String): Errors = Errors(Seq(Error(message)))
}
