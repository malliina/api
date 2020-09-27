package com.malliina.mavenapi

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.{QueryParamEncoder, QueryParameterValue}

object MavenCentralClient {
  def apply(http: HttpClient): MavenCentralClient = new MavenCentralClient(http)

  implicit class QueryEncoderOps[T](enc: QueryParamEncoder[T]) {
    def map(f: String => String): QueryParamEncoder[T] = (value: T) => QueryParameterValue(f(enc.encode(value).value))
  }
}

/**
  * @param http http client
  *
  * @see https://blog.sonatype.com/2011/06/you-dont-need-a-browser-to-use-maven-central/
  */
class MavenCentralClient(http: HttpClient) {
  val baseUrl = uri"https://search.maven.org/solrsearch/select"

  def search(q: MavenQuery): IO[MavenSearchResults] = {
    val url =
      baseUrl.withQueryParams(
        Map("q" -> s"""g:"${q.group}" AND a:"${q.scalaArtifactName}"""", "rows" -> "20", "core" -> "gav")
      )
    http.run[MavenSearchResponse](GET(url)).map { res =>
      MavenSearchResults(res.response.docs)
    }
  }
}
