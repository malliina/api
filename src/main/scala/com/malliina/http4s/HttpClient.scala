package com.malliina.http4s

import cats.effect.{ContextShift, IO}
import org.http4s.Method._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.{AuthScheme, Credentials, EntityDecoder, Request}

import scala.concurrent.ExecutionContext

object HttpClient {
  def apply(ec: ExecutionContext, ctx: ContextShift[IO]): HttpClient = new HttpClient(ec)(ctx)
}

class HttpClient(ec: ExecutionContext)(implicit ctx: ContextShift[IO]) {
  val resource = BlazeClientBuilder[IO](ec).resource

  def demo: IO[String] = {
    val request = GET(
      uri"https://www.google.com/",
      Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))
    )
    run[String](request)
  }

  def run[T](req: IO[Request[IO]])(implicit ev: EntityDecoder[IO, T]): IO[T] = resource.use { client =>
    req.flatMap(client.run(_).use { res =>
      if (res.status.isSuccess) {
        res.attemptAs[T].value.flatMap { e =>
          e.fold(err => IO.raiseError(err), t => IO.pure(t))
        }
      } else {
        IO.raiseError(new Exception(s"Invalid status code: '${res.status.code}'."))
      }
    })
  }

  def runExpect[T](req: IO[Request[IO]])(implicit ev: EntityDecoder[IO, T]) = resource.use { client =>
    client.expect[T](req)
  }
}
