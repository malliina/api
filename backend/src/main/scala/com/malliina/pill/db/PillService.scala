package com.malliina.pill.db

import cats.Monad
import cats.effect.IO

class PillService[F[_]: Monad](db: DatabaseRunner[F]):
  def save(in: PillInput): F[PillRow] = ???
