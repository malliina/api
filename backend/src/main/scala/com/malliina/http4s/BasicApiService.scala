package com.malliina.http4s

import cats.Applicative
import cats.data.NonEmptyList
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.headers.`Cache-Control`

object BasicApiService:
  val noCacheDirectives = NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
  val noCache = `Cache-Control`(noCacheDirectives)

class BasicApiService[F[_]: Applicative]
  extends BasicService[F]
  with MyScalatagsInstances
  with Extractors
