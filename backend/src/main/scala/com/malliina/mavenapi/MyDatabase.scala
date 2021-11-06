package com.malliina.mavenapi

import cats.effect.IO

object MyDatabase:
  val result = "The result!"

  def apply(): MyDatabase = new MyDatabase()

class MyDatabase():
  def load: IO[AppResult] = IO.pure(AppResult.example)
