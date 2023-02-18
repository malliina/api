package com.malliina.pill.db

import com.malliina.config.ConfigReadable
import com.malliina.pill.PillConf.ConfigOps

case class DatabaseConf(url: String, user: String, pass: String, migrateOnStart: Boolean)

object DatabaseConf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  implicit val config: ConfigReadable[DatabaseConf] = ConfigReadable.config.emap { obj =>
    for
      url <- obj.read[String]("url")
      user <- obj.read[String]("user")
      pass <- obj.read[String]("pass")
      migrateOnStart <- obj.read[Boolean]("migrateOnStart")
    yield DatabaseConf(url, user, pass, migrateOnStart)
  }
