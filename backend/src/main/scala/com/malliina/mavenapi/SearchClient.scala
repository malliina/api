package com.malliina.mavenapi

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxParallelTraverse1, toFunctorOps}
import com.malliina.http.HttpClient
import com.malliina.mavenapi.ScalaVersion.{sbt1, scala213, scala3, sjs1scala213, sjs1scala3}
import com.malliina.util.AppLogger

// Maven Central (old) or Sonatype Central (new)
trait ArtifactSearcher[F[_]]:
  def searchByVersion(q: MavenQuery): F[SearchResults]

object SearchClient:
  def central[F[_]: {Async, Parallel}](http: HttpClient[F]) =
    SearchClient(SonatypeCentralClient[F](http))

class SearchClient[F[_]: {Async, Parallel}](searcher: ArtifactSearcher[F]):
  private val log = AppLogger(getClass)

  def search(q: MavenQuery): F[SearchResults] =
    (NonEmptyList
      .of(scala3, scala213, sjs1scala213, sjs1scala3, sbt1)
      .map(sv => q.copy(scalaVersion = Option(sv))) ++ List(q.copy(scalaVersion = None)))
      .parTraverse(query => searchByVersionWithRetry(query))
      .map(list => SearchResults(list.toList.flatMap(_.results).sortBy(_.timestamp).reverse))

  private def searchByVersionWithRetry(q: MavenQuery): F[SearchResults] =
    searcher
      .searchByVersion(q)
      .handleErrorWith:
        case te: TimeoutException =>
          log.warn(s"Request timeout for '${te.url}', retrying...")
          searcher.searchByVersion(q)
        case other => Async[F].raiseError(other)
