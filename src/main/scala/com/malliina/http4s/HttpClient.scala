package com.malliina.http4s

import cats.effect.{ContextShift, IO}
import org.http4s.Method._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.{AuthScheme, Credentials, EntityDecoder, MediaType, Request}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object HttpClient {
  def apply(ec: ExecutionContext, ctx: ContextShift[IO]): HttpClient = new HttpClient(ec)(ctx)
}

class HttpClient(ec: ExecutionContext)(implicit ctx: ContextShift[IO]) {
  val resource = BlazeClientBuilder[IO](global).resource

  def demo: IO[String] = {
    val request = GET(
      uri"https://www.google.com/",
      Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))
    )
    run[String](request)
  }

  def run[T](req: IO[Request[IO]])(implicit ev: EntityDecoder[IO, T]) = resource.use { client =>
    client.fetch(req) { r =>
      if (r.status.isSuccess) {
        r.attemptAs[T].value.flatMap { e =>
          e.fold(err => IO.raiseError(err), str => IO.pure(str))
        }
      } else {
        IO.raiseError(new Exception(s"Invalid status code: '${r.status.code}'."))
      }
    }
  }

  def runExpect[T](req: IO[Request[IO]])(implicit ev: EntityDecoder[IO, T]) = resource.use { client =>
    client.expect[T](req)
  }
}
