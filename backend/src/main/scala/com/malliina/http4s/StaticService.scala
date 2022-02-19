package com.malliina.http4s

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.kernel.Sync
import cats.implicits.*
import com.malliina.http4s.StaticService.log
import com.malliina.mavenapi.BuildInfo
import com.malliina.values.UnixPath
import com.malliina.util.AppLogger
import org.http4s.CacheDirective.{`max-age`, `no-cache`, `public`}
import org.http4s.headers.`Cache-Control`
import org.http4s.{HttpRoutes, Request, StaticFile}
import org.slf4j.LoggerFactory

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: Async] extends BasicService[F]:
  val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico") ++ fontExtensions

  val publicDir = fs2.io.file.Path(BuildInfo.assetsDir)
  val routes = HttpRoutes.of[F] {
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = UnixPath(rest.segments.mkString("/"))
      val isCacheable = file.value.count(_ == '.') == 2
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`())
      log.info(s"Searching for '$file' in '$publicDir'...")
//      StaticFile.fromResource(res, Option(req))
      StaticFile
        .fromPath(publicDir.resolve(file.value), Option(req))
        .map(_.putHeaders(`Cache-Control`(cacheHeaders)))
        .fold(onNotFound(req))(_.pure[F])
        .flatten
  }

  private def onNotFound(req: Request[F]) =
    log.info(s"Not found '${req.uri}'.")
    notFound(req)
