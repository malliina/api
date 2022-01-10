package com.malliina.pill.db

import cats.implicits.*
import cats.Monad
import cats.effect.IO
import com.malliina.pill.{EnablePillNotifications, DisablePillNotifications}
import doobie.implicits.*
import doobie.util.log.LogHandler

class PillService[F[_]: Monad](db: DatabaseRunner[F]):
  implicit val logger: LogHandler = db.logHandler

  def enable(in: EnablePillNotifications): F[PillRow] = db.run {
    for
      id <- sql"""insert into push_clients(token, device) values(${in.token}, ${in.os})""".update
        .withUniqueGeneratedKeys[PillRowId]("id")
      row <- sql"""select id, token, device, added from push_clients where id = $id"""
        .query[PillRow]
        .unique
    yield row
  }

  def disable(req: DisablePillNotifications): F[Int] = db.run {
    sql"""delete from push_clients where token = ${req.token}""".update.run
  }
