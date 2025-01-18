package com.malliina.pill.db

import cats.Monad
import com.malliina.database.DoobieDatabase
import com.malliina.pill.db.PillService.log
import com.malliina.pill.{DisablePillNotifications, EnablePillNotifications}
import com.malliina.util.AppLogger
import doobie.implicits.*

object PillService:
  private val log = AppLogger(getClass)

class PillService[F[_]: Monad](db: DoobieDatabase[F]):
  def enable(in: EnablePillNotifications): F[PillRow] = db.run:
    for
      id <- sql"""insert into push_clients(token, device) values(${in.token}, ${in.os})""".update
        .withUniqueGeneratedKeys[PillRowId]("id")
      row <- sql"""select id, token, device, added from push_clients where id = $id"""
        .query[PillRow]
        .unique
    yield
      log.info(s"Saved ${in.os} token '${in.token}' with ID ${row.id}.")
      row

  def disable(req: DisablePillNotifications): F[Int] = db.run:
    sql"""delete from push_clients where token = ${req.token}""".update.run.map: int =>
      if int > 0 then log.info(s"Removed token '${req.token}'.")
      else log.info(s"Tried to remove token '${req.token}', but did not find it.")
      int
