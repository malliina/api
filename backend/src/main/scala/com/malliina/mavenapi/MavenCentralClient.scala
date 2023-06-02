package com.malliina.mavenapi

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.{Async, IO, Resource}
import cats.implicits.*
import com.malliina.http.FullUrl
import com.malliina.http.io.{HttpClientF2, HttpClientIO}
import com.malliina.mavenapi.ScalaVersion.*
import com.malliina.util.AppLogger
import io.circe.Json
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.*
import org.http4s.{QueryParamEncoder, QueryParameterValue}
import org.slf4j.LoggerFactory

import java.net.SocketTimeoutException

object MavenCentralClient:
  implicit class QueryEncoderOps[T](enc: QueryParamEncoder[T]):
    def map(f: String => String): QueryParamEncoder[T] = (value: T) =>
      QueryParameterValue(f(enc.encode(value).value))

/** @param http
  *   http client
  *
  * @see
  *   https://blog.sonatype.com/2011/06/you-dont-need-a-browser-to-use-maven-central/
  */
class MavenCentralClient[F[_]: Async: Parallel](http: HttpClientF2[F]):
  private val log = AppLogger(getClass)

  private val baseUrl = FullUrl.https("search.maven.org", "/solrsearch/select")

  def searchWildcard(q: String): F[Json] =
    val url = baseUrl.withQuery("q" -> q, "rows" -> "20", "wt" -> "json")
    http.getAs[Json](url)

  def search(q: MavenQuery): F[MavenSearchResults] =
    (NonEmptyList
      .of(scala3, scala213, sjs1scala213, sjs1scala3)
      .map(sv => q.copy(scalaVersion = Option(sv))) ++ List(q.copy(scalaVersion = None)))
      .parTraverse(query => searchByVersionWithRetry(query))
      .map(list => MavenSearchResults(list.toList.flatMap(_.results).sortBy(_.timestamp).reverse))

  private def searchByVersionWithRetry(q: MavenQuery): F[MavenSearchResults] =
    searchByVersion(q).handleErrorWith {
      case te: TimeoutException =>
        log.warn(s"Request timeout for '${te.url}', retrying...")
        searchByVersion(q)
      case other => Async[F].raiseError(other)
    }

  private def searchByVersion(q: MavenQuery): F[MavenSearchResults] =
    val group = q.group.map(g => s"""g:"$g"""")
    val artifact = q.artifactName.map(a => s"""a:"$a"""")
    val searchQuery = (group.toList ++ artifact.toList).mkString(" AND ")
    val url = baseUrl.withQuery(
      "q" -> searchQuery,
      "rows" -> "20",
      "core" -> "gav"
    )
    log.info(s"Fetching '$url'...")
    http
      .getAs[MavenSearchResponse](url)
      .map { res =>
        log.info(s"Found ${res.response.numFound} artifacts from '$url'.")
        MavenSearchResults(res.response.docs)
      }
      .adaptError { case ste: SocketTimeoutException =>
        log.info(s"Request timeout for '$url'.")
        TimeoutException(url, ste)
      }
