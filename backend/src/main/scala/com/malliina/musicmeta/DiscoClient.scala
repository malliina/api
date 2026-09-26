package com.malliina.musicmeta

import cats.effect.Async
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import com.malliina.http.{FullUrl, HttpClient, ResponseException}
import com.malliina.musicmeta.DiscoClient.log
import com.malliina.storage.*
import com.malliina.util.AppLogger
import com.malliina.values.{AccessToken, NonBlank}
import org.apache.commons.codec.digest.DigestUtils

import java.nio.file.{Files, Path, Paths}

object DiscoClient:
  private val log = AppLogger(getClass)

  private val tempDir = Paths.get(sys.props("java.io.tmpdir"))
  private val fallbackCoverDir = tempDir.resolve("covers")
  private val coverDir = sys.props.get("cover.dir").fold(fallbackCoverDir)(path => Paths.get(path))

class DiscoClient[F[_]: Async](
  token: AccessToken,
  http: HttpClient[F],
  coverDir: Path = DiscoClient.coverDir
):
  val F: Async[F] = Async[F]
  private val Authorization = "Authorization"
  Files.createDirectories(coverDir)
  private val iLoveDiscoGsFakeCoverSize = 15378

  /** Returns the album cover. Optionally downloads and caches it if it doesn't already exist
    * locally.
    *
    * Fails with a [[NoSuchElementException]] if the cover cannot be found. Can also fail with a
    * [[java.io.IOException]].
    *
    * @return
    *   the album cover file, which is an image
    */
  def cover(artist: NonBlank, album: Option[NonBlank]): F[Path] =
    val file = coverFile(artist, album)
    if Files.isReadable(file) && Files.size(file) != iLoveDiscoGsFakeCoverSize then F.pure(file)
    else
      downloadCover(artist, album).flatMap: f =>
        if Files.size(f) != iLoveDiscoGsFakeCoverSize then F.pure(f)
        else
          log.error(s"Failed to download cover of '$artist' - '$album'.")
          F.raiseError(
            new NoSuchElementException(s"Fake cover of size $iLoveDiscoGsFakeCoverSize bytes.")
          )

  private def downloadCover(artist: NonBlank, album: Option[NonBlank]): F[Path] =
    downloadCover(artist, album, _ => coverFile(artist, album))

  /** Streams `url` to `file`.
    *
    * @param url
    *   url to download
    * @param file
    *   destination path
    * @return
    *   the size of the downloaded file, stored in `file`
    * @see
    *   http://www.playframework.com/documentation/2.6.x/ScalaWS
    */
  private def downloadFile(url: FullUrl, file: Path): F[StorageSize] =
    http
      .download(url, file, Map(Authorization -> authValue))
      .flatMap: either =>
        either.fold(
          err => F.raiseError(new ResponseException(err)),
          s => F.pure(s)
        )

  private def coverFile(artist: NonBlank, album: Option[NonBlank]): Path =
    // avoids platform-specific file system encoding nonsense
    val hash = DigestUtils.md5Hex(s"$artist-$album")
    coverDir.resolve(s"$hash.jpg")

  /** Downloads the album cover of `artist`s `album`.
    *
    * Performs three web requests in sequence to the DiscoGs API:
    *
    * 1) Obtains the album ID 2) Obtains the album details (with the given album ID) 3) Downloads
    * the album cover (the URL of which is available in the details)
    *
    * At least the last step, which downloads the cover, requires OAuth authentication.
    *
    * @param artist
    *   the artist
    * @param album
    *   the album
    * @param fileFor
    *   the file to download the cover to, given its remote URL
    * @return
    *   the downloaded album cover along with the number of bytes downloaded
    */
  private def downloadCover(
    artist: NonBlank,
    album: Option[NonBlank],
    fileFor: FullUrl => Path
  ): F[Path] =
    for
      url <- albumCoverForSearch(albumIdUrl(artist, album))
      file = fileFor(url)
      size <- downloadFile(url, file)
    yield
      log.info(s"Downloaded cover for '$artist - $album', $size to ${file.toAbsolutePath}.")
      file

  private def albumCoverForSearch(url: FullUrl): F[FullUrl] =
    http
      .getAs[CoverSearchResult](url, Map(Authorization -> authValue))
      .flatMap: result =>
        result.results
          .map(_.cover_image)
          .headOption
          .map(F.pure)
          .getOrElse:
            F.raiseError(CoverNotFoundException(s"Unable to find cover image from $url."))

  private def albumIdUrl(artist: NonBlank, album: Option[NonBlank]): FullUrl =
    val artistQuery = Map("artist" -> artist)
    val query = album.fold(artistQuery): album =>
      artistQuery ++ Map("release_title" -> album)
    FullUrl
      .https("api.discogs.com", "/database/search")
      .query(query)

  private def authValue = s"Discogs token=$token"
