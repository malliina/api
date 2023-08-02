package com.malliina.pill.db

import doobie.*

import java.time.Instant

object Mappings extends Mappings

trait Mappings:
  given Meta[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta
  given Meta[PushToken] = Meta[String].timap(PushToken.apply)(_.value)
  given Meta[MobileOS] = Meta[String].timap(MobileOS.unsafe)(_.name)
  given Meta[PillRowId] = Meta[Long].timap(PillRowId.apply)(_.id)
