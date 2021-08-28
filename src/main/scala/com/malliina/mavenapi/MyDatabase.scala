package com.malliina.mavenapi

import cats.effect.{ContextShift, IO}

import scala.concurrent.{ExecutionContext, Future}

object MyDatabase {
  val result = "The result!"

  def apply(ec: ExecutionContext, ctx: ContextShift[IO]): MyDatabase = new MyDatabase()(ec, ctx)
}

class MyDatabase()(implicit ec: ExecutionContext, ctx: ContextShift[IO]) {
  def load: IO[AppResult] = IO.pure(AppResult.example)
}
