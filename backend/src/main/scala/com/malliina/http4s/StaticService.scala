package com.malliina.http4s

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.*
import com.malliina.http4s.StaticService.log
import com.malliina.mavenapi.BuildInfo
import com.malliina.values.UnixPath
import com.malliina.util.AppLogger
import org.http4s.CacheDirective.{`max-age`, `no-cache`, `public`, `no-store`, `must-revalidate`}
import org.http4s.headers.`Cache-Control`
import org.http4s.{Header, HttpRoutes, Request, StaticFile}
import org.typelevel.ci.CIStringSyntax

import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: Async] extends BasicService[F]:
  private val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  private val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico", ".svg") ++ fontExtensions

  private val allowAllOrigins = Header.Raw(ci"Access-Control-Allow-Origin", "*")
  private val assetsDir = fs2.io.file.Path(BuildInfo.assetsDir.getAbsolutePath)
  private val publicDir = fs2.io.file.Path(BuildInfo.publicDir)
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = UnixPath(rest.segments.mkString("/"))
      val isCacheable = file.value.count(_ == '.') == 2
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
      val search =
        if BuildInfo.isProd then
          val resourcePath = s"${BuildInfo.publicFolder}/${file.value}"
          log.debug(s"Searching for resource '$resourcePath'...")
          StaticFile.fromResource(resourcePath, Option(req))
        else
          val assetPath: fs2.io.file.Path = assetsDir.resolve(file.value)
          val publicPath = publicDir.resolve(file.value)
          log.debug(
            s"Searching for file '${assetPath.toNioPath.toAbsolutePath}' or '${publicPath.toNioPath.toAbsolutePath}'..."
          )
          StaticFile
            .fromPath(assetPath, Option(req))
            .orElse(StaticFile.fromPath(publicPath, Option(req)))
      search
        .map(_.putHeaders(`Cache-Control`(cacheHeaders), allowAllOrigins))
        .fold(onNotFound(req))(_.pure[F])
        .flatten
  }

  private def onNotFound(req: Request[F]) =
    log.info(s"Not found '${req.uri}'.")
    notFound(req)
