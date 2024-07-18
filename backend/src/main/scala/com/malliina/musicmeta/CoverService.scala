package com.malliina.musicmeta

import cats.effect.Async
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.http.{Errors, ResponseException}
import com.malliina.http4s.BasicApiService.noCache
import com.malliina.http4s.{AppImplicits, QueryParsers}
import com.malliina.musicmeta.CoverService.log
import com.malliina.util.AppLogger
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder

import java.net.ConnectException

object CoverService:
  private val log = AppLogger(getClass)

class CoverService[F[_]: Async](disco: DiscoClient[F]) extends AppImplicits[F]:
  val F = Async[F]
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      val e = for
        artist <- QueryParsers.parse[String](req.uri.query, "artist")
        album <- QueryParsers.parse[String](req.uri.query, "album")
      yield
        val coverName = s"$artist - $album"
        disco
          .cover(artist, album)
          .flatMap(file => Ok(file))
          .handleErrorWith:
            case _: CoverNotFoundException =>
              val userMessage = s"Unable to find cover '$coverName'."
              log.info(userMessage)
              notFound(userMessage)
            case _: NoSuchElementException =>
              val userMessage = s"Unable to find cover '$coverName'."
              log.info(userMessage)
              notFound(userMessage)
            case re: ResponseException =>
              log.error(s"Invalid response received.", re)
              BadGateway(Errors("Invalid response received."), noCache)
            case ce: ConnectException =>
              log.warn(
                s"Unable to search for cover '$coverName'. Unable to connect to cover backend: ${ce.getMessage}",
                ce
              )
              BadGateway(Errors("Unable to connect."), noCache)
            case t =>
              log.error(s"Failure while searching cover '$coverName'.")
              InternalServerError(Errors("Internal error. My bad."))
      e.fold(
        errors => BadRequest(errors, noCache),
        ok => ok
      )

  private def notFound(str: String) = NotFound(Errors(str), noCache)
