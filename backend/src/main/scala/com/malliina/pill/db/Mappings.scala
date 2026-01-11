package com.malliina.pill.db

import cats.Show
import com.malliina.values.ValidatingCompanion
import doobie.*

import java.time.Instant

object Mappings extends Mappings

trait Mappings:
  given Meta[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta
  given Meta[PushToken] = validated(PushToken)
  given Meta[MobileOS] = validated(MobileOS)
  given Meta[PillRowId] = validated(PillRowId)

  private def validated[T, R: {Meta, Show}, C <: ValidatingCompanion[R, T]](c: C): Meta[T] =
    Meta[R].tiemap(r => c.build(r).left.map(err => err.message))(c.write)
