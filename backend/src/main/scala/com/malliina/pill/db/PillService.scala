package com.malliina.pill.db

import cats.Monad
import com.malliina.database.DoobieDatabase
import com.malliina.pill.{DisablePillNotifications, EnablePillNotifications}
import doobie.implicits.*

class PillService[F[_]: Monad](db: DoobieDatabase[F]):
  def enable(in: EnablePillNotifications): F[PillRow] = db.run:
    for
      id <- sql"""insert into push_clients(token, device) values(${in.token}, ${in.os})""".update
        .withUniqueGeneratedKeys[PillRowId]("id")
      row <- sql"""select id, token, device, added from push_clients where id = $id"""
        .query[PillRow]
        .unique
    yield row

  def disable(req: DisablePillNotifications): F[Int] = db.run:
    sql"""delete from push_clients where token = ${req.token}""".update.run
