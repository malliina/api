package com.malliina.mavenapi

import cats.Applicative

object MyDatabase:
  val result = "The result!"

class MyDatabase[F[_]: Applicative]:
  def load: F[AppResult] = Applicative[F].pure(AppResult.example)
