package com.malliina.mavenapi

import cats.effect.{ContextShift, IO, Resource}
import org.http4s.Method.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.{AuthScheme, Credentials, EntityDecoder, Request}

import scala.concurrent.ExecutionContext

object HttpClient {
  def apply(ec: ExecutionContext)(implicit ctx: ContextShift[IO]): Resource[IO, HttpClient] = for {
    http <- BlazeClientBuilder[IO](ec).resource
  } yield new HttpClient(http)
}

class HttpClient(http: Client[IO]) {
  def demo: IO[String] = {
    val request = GET(
      uri"https://www.google.com/",
      Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))
    )
    run[String](request)
  }

  def run[T](req: Request[IO])(implicit ev: EntityDecoder[IO, T]): IO[T] =
    http.run(req).use { res =>
      if (res.status.isSuccess) {
        res.attemptAs[T].value.flatMap { e =>
          e.fold(err => IO.raiseError(err), t => IO.pure(t))
        }
      } else {
        IO.raiseError(new Exception(s"Invalid status code: '${res.status.code}'."))
      }
    }

  def runExpect[T](req: IO[Request[IO]])(implicit ev: EntityDecoder[IO, T]) =
    http.expect[T](req)
}
