package com.malliina.pill.db

import cats.Monad
import com.malliina.pill.db.Mappings.*
import doobie.*
import doobie.implicits.*
import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}

class PillDatabase[F[_]: Monad](db: DatabaseRunner[F]):
  implicit val logHandler: LogHandler = db.logHandler

  def enable(in: PillInput): F[PillRow] =
    val insertion = sql"""insert into push_clients(token, device) values(${in.token}, ${in.os})"""
      .update(logHandler)
      .withUniqueGeneratedKeys[PillRowId]("id")
    val io = for
      id <- insertion
      row <- sql"""select id, token, os from push_clients where id = $id""".query[PillRow].unique
    yield row
    db.run(io)
