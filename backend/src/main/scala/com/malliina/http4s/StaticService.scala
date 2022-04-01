package com.malliina.http4s

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.kernel.Sync
import cats.implicits.*
import com.malliina.http4s.StaticService.log
import com.malliina.mavenapi.BuildInfo
import com.malliina.values.UnixPath
import com.malliina.util.AppLogger
import org.http4s.CacheDirective.{`max-age`, `no-cache`, `public`, `no-store`, `must-revalidate`}
import org.http4s.headers.`Cache-Control`
import org.http4s.{Header, HttpRoutes, Request, StaticFile}
import org.slf4j.LoggerFactory
import org.typelevel.ci.CIStringSyntax

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: Async] extends BasicService[F]:
  val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico") ++ fontExtensions

  private val allowAllOrigins = Header.Raw(ci"Access-Control-Allow-Origin", "*")
  val publicDir = fs2.io.file.Path(BuildInfo.assetsDir)
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = UnixPath(rest.segments.mkString("/"))
      val isCacheable = file.value.count(_ == '.') == 2
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
      val resourcePath = s"${BuildInfo.publicFolder}/${file.value}"
      log.debug(s"Searching for $resourcePath in resources or else '$file' in '$publicDir'...")
      val search =
        if BuildInfo.isProd then StaticFile.fromResource(resourcePath, Option(req))
        else StaticFile.fromPath(publicDir.resolve(file.value), Option(req))
      search
        .map(_.putHeaders(`Cache-Control`(cacheHeaders), allowAllOrigins))
        .fold(onNotFound(req))(_.pure[F])
        .flatten
  }

  private def onNotFound(req: Request[F]) =
    log.info(s"Not found '${req.uri}'.")
    notFound(req)
