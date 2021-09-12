package com.malliina.mavenapi

import cats.effect.IO
import cats.data.NonEmptyList
import cats.implicits.*
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.dsl.io.*
import org.http4s.implicits.*
import org.http4s.{QueryParamEncoder, QueryParameterValue}
import org.slf4j.LoggerFactory

object MavenCentralClient:
  def apply(http: HttpClient): MavenCentralClient = new MavenCentralClient(http)

  implicit class QueryEncoderOps[T](enc: QueryParamEncoder[T]):
    def map(f: String => String): QueryParamEncoder[T] = (value: T) =>
      QueryParameterValue(f(enc.encode(value).value))

/** @param http
  *   http client
  *
  * @see
  *   https://blog.sonatype.com/2011/06/you-dont-need-a-browser-to-use-maven-central/
  */
class MavenCentralClient(http: HttpClient):
  private val log = LoggerFactory.getLogger(getClass)

  val baseUrl = uri"https://search.maven.org/solrsearch/select"

  def search(q: MavenQuery): IO[MavenSearchResults] =
    val scala3 = search2(q.copy(scalaVersion = ScalaVersion.scala3))
    val scala213 = search2(q.copy(scalaVersion = ScalaVersion.scala213))
    NonEmptyList
      .of(scala3, scala213)
      .traverse(identity)
      .map(list => MavenSearchResults(list.toList.flatMap(_.results).sortBy(_.timestamp).reverse))

  def search2(q: MavenQuery): IO[MavenSearchResults] =
    val group = q.group.map(g => s"""g:"$g"""")
    val artifact = q.scalaArtifactName.map(a => s"""a:"$a"""")
    val searchQuery = (group.toList ++ artifact.toList).mkString(" AND ")
    val url =
      baseUrl.withQueryParams(
        Map(
          "q" -> searchQuery,
          "rows" -> "20",
          "core" -> "gav"
        )
      )
    log.debug(s"Fetch '$url'.")
    http.run[MavenSearchResponse](GET(url)).map { res =>
      MavenSearchResults(res.response.docs)
    }
