package com.malliina.mavenapi

import cats.effect.Async
import cats.implicits.*
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.util.AppLogger

import java.net.SocketTimeoutException

/** @param http
  *   http client
  *
  * @see
  *   https://blog.sonatype.com/2011/06/you-dont-need-a-browser-to-use-maven-central/
  */
class MavenCentralClient[F[_]: Async](http: HttpClient[F]) extends ArtifactSearcher[F]:
  private val log = AppLogger(getClass)

  private val baseUrl = FullUrl.https("search.maven.org", "/solrsearch/select")

  override def searchByVersion(q: MavenQuery): F[SearchResults] =
    val group = q.group.map(g => s"""g:"$g"""")
    val artifact = q.artifactName.map(a => s"""a:"$a"""")
    val searchQuery = (group.toList ++ artifact.toList).mkString(" AND ")
    val url = baseUrl.query(
      Map(
        "q" -> searchQuery,
        "rows" -> "20",
        "core" -> "gav"
      )
    )
    log.info(s"Fetching '$url'...")
    http
      .getAs[MavenSearchResponse](url)
      .map: res =>
        log.info(s"Found ${res.response.numFound} artifacts from '$url'.")
        SearchResults(res.response.docs)
      .adaptError:
        case ste: SocketTimeoutException =>
          log.info(s"Request timeout for '$url'.")
          TimeoutException(url, ste)
