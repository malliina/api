package com.malliina.mavenapi

import cats.effect.IO

import scala.concurrent.{ExecutionContext, Future}

object MyDatabase:
  val result = "The result!"

  def apply(ec: ExecutionContext): MyDatabase = new MyDatabase()(ec)

class MyDatabase()(implicit ec: ExecutionContext):
  def load: IO[AppResult] = IO.pure(AppResult.example)
