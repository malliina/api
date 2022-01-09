package com.malliina.pill.db

import doobie.*

import java.time.Instant

object Mappings extends Mappings

trait Mappings:
  implicit val instantMapping: Meta[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta
  implicit val tokenMapping: Meta[PushToken] = Meta[String].timap(PushToken.apply)(_.value)
  implicit val osMapping: Meta[MobileOS] = Meta[String].timap(MobileOS.unsafe)(_.name)
  implicit val idMapping: Meta[PillRowId] = Meta[Long].timap(PillRowId.apply)(_.id)
