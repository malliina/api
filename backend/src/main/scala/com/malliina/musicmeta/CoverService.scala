package com.malliina.musicmeta

import cats.effect.Async
import cats.syntax.all.{catsSyntaxApplicativeError, toFlatMapOps}
import com.malliina.http.{Errors, ResponseException}
import com.malliina.http4s.BasicApiService.noCache
import com.malliina.http4s.{AppImplicits, QueryParsers}
import com.malliina.musicmeta.CoverService.{CoverSearch, log}
import com.malliina.util.AppLogger
import com.malliina.values.{NonBlank, StringEnumCompanion}
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder, Uri}

import java.net.ConnectException

object CoverService:
  private val log = AppLogger(getClass)

  enum SearchAccuracy(val name: String):
    case Exact extends SearchAccuracy("exact")
    case BestEffort extends SearchAccuracy("bestEffort")

  object SearchAccuracy extends StringEnumCompanion[SearchAccuracy]:
    val default: SearchAccuracy = Exact
    override def all: Seq[SearchAccuracy] = Seq(Exact, BestEffort)
    override def write(t: SearchAccuracy): String = t.name

    given QueryParamDecoder[SearchAccuracy] = QueryParamDecoder.stringQueryParamDecoder.emap: s =>
      build(s).left.map(err => ParseFailure(err.message, err.message))

  case class CoverSearch(artist: NonBlank, album: Option[NonBlank], accuracy: SearchAccuracy):
    def coverName: String = album.fold(NonBlank.write(artist))(alb => s"$artist - $alb")

  given QueryParamDecoder[NonBlank] = QueryParamDecoder.stringQueryParamDecoder.emap: s =>
    NonBlank.build(s).left.map(err => ParseFailure(err.message, err.message))

  object CoverSearch:
    def fromUri(uri: Uri) =
      for
        artist <- QueryParsers.parse[NonBlank](uri.query, "artist")
        album <- QueryParsers.parseOptE[NonBlank](uri.query, "album")
        accuracy <- QueryParsers.parseOrDefault(uri.query, "accuracy", SearchAccuracy.default)
      yield CoverSearch(artist, album, accuracy)

class CoverService[F[_]: Async](disco: DiscoClient[F]) extends AppImplicits[F]:
  val F = Async[F]
  val service: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      parsed(CoverSearch.fromUri(req.uri)): q =>
        val coverName = q.coverName
        disco
          .cover(q)
          .flatMap(file => Ok(file))
          .handleErrorWith:
            case e: CoverNotFoundException =>
              val userMessage = s"Unable to find cover '$coverName'."
              log.info(userMessage, e)
              notFound(userMessage)
            case nsee: NoSuchElementException =>
              val userMessage = s"No cover for '$coverName'."
              log.info(userMessage, nsee)
              notFound(userMessage)
            case re: ResponseException =>
              log.error(s"Invalid response received.", re)
              badGateway("Invalid response received.")
            case ce: ConnectException =>
              log.warn(
                s"Unable to search for cover '$coverName'. Unable to connect to cover backend: ${ce.getMessage}",
                ce
              )
              badGateway("Unable to connect.")
            case t =>
              log.error(s"Failure while searching cover '$coverName'.", t)
              InternalServerError(Errors("Internal error. My bad."))

  private def notFound(str: String) = NotFound(Errors(str), noCache)
  private def badGateway(str: String) = BadGateway(Errors(str), noCache)
