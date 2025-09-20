package com.malliina.mavenapi

import cats.effect.Async
import cats.implicits.*
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.util.AppLogger

import java.net.SocketTimeoutException

class SonatypeCentralClient[F[_]: Async](http: HttpClient[F]) extends ArtifactSearcher[F]:
  private val log = AppLogger(getClass)

  private val baseUrl =
    FullUrl.https("central.sonatype.com", "/api/internal/browse/component/versions")

  override def searchByVersion(q: MavenQuery): F[SearchResults] =
    val group = q.group.map(g => s"""namespace:$g""")
    val artifact = q.artifactName.map(a => s"""name:$a""")
    val searchQuery = (group.toList ++ artifact.toList).mkString(",")
    val url = baseUrl.query(
      Map(
        "sortField" -> "normalizedVersion",
        "sortDirection" -> "desc",
        "page" -> "0",
        "size" -> "20",
        "filter" -> searchQuery
      )
    )
    log.info(s"Fetching '$url'...")
    http
      .getAs[CentralResponse](url)
      .map: res =>
        log.info(s"Found ${res.totalResultCount} artifacts from '$url'.")
        res.toResults
      .adaptError:
        case ste: SocketTimeoutException =>
          log.info(s"Request timeout for '$url'.")
          TimeoutException(url, ste)
