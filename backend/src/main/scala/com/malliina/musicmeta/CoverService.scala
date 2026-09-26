package com.malliina.musicmeta

import cats.effect.Async
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.http.{Errors, ResponseException}
import com.malliina.http4s.BasicApiService.noCache
import com.malliina.http4s.{AppImplicits, QueryParsers}
import com.malliina.musicmeta.CoverService.{CoverSearch, log}
import com.malliina.util.AppLogger
import com.malliina.values.NonBlank
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder, Uri}
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder

import java.net.ConnectException

object CoverService:
  private val log = AppLogger(getClass)

  case class CoverSearch(artist: NonBlank, album: Option[NonBlank]):
    def coverName = s"$artist - $album"

  given QueryParamDecoder[NonBlank] = QueryParamDecoder.stringQueryParamDecoder.emap: s =>
    NonBlank.build(s).left.map(err => ParseFailure(err.message, err.message))

  object CoverSearch:
    def fromUri(uri: Uri) =
      for
        artist <- QueryParsers.parse[NonBlank](uri.query, "artist")
        album <- QueryParsers.parseOptE[NonBlank](uri.query, "album")
      yield CoverSearch(artist, album)

class CoverService[F[_]: Async](disco: DiscoClient[F]) extends AppImplicits[F]:
  val F = Async[F]
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      parsed(CoverSearch.fromUri(req.uri)): q =>
        val coverName = q.coverName
        disco
          .cover(q.artist, q.album)
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
              log.error(s"Failure while searching cover '$coverName'.", t)
              InternalServerError(Errors("Internal error. My bad."))

  private def notFound(str: String) = NotFound(Errors(str), noCache)
