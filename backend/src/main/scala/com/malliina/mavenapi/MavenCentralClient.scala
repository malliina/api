package com.malliina.mavenapi

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
class MavenCentralClient[F[_]: Async](http: HttpClientF2[F]):
  private val log = AppLogger(getClass)

  val baseUrl = FullUrl.https("search.maven.org", "/solrsearch/select")

  def searchWildcard(q: String): F[Json] =
    val url = baseUrl.withQuery("q" -> q, "rows" -> "20", "wt" -> "json")
    http.getAs[Json](url)

  def search(q: MavenQuery): F[MavenSearchResults] =
    NonEmptyList
      .of(scala3, scala213, sjs1scala213, sjs1scala3)
      .traverse(sv => searchByVersion(q.copy(scalaVersion = sv)))
      .map(list => MavenSearchResults(list.toList.flatMap(_.results).sortBy(_.timestamp).reverse))

  def searchByVersion(q: MavenQuery): F[MavenSearchResults] =
    val group = q.group.map(g => s"""g:"$g"""")
    val artifact = q.scalaArtifactName.map(a => s"""a:"$a"""")
    val searchQuery = (group.toList ++ artifact.toList).mkString(" AND ")
    val url = baseUrl.withQuery(
      "q" -> searchQuery,
      "rows" -> "20",
      "core" -> "gav"
    )
    log.info(s"Fetching '$url'...")
    http.getAs[MavenSearchResponse](url).map { res =>
      MavenSearchResults(res.response.docs)
    }
