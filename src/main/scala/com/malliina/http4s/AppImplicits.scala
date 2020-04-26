package com.malliina.http4s

import cats.effect.IO
import io.circe.generic.AutoDerivation
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.play.PlayInstances
import org.http4s.scalatags.ScalatagsInstances
import org.http4s.syntax

trait Extractors {
  object UserIdVar {
    def unapply(str: String): Option[UserId] =
      if (str.trim.nonEmpty) Option(UserId(str.trim)) else None
  }
}

trait AppImplicits
  extends syntax.AllSyntaxBinCompat
  with Http4sDsl[IO]
  with CirceInstances
//  with PlayInstances
  with ScalatagsInstances
  with Extractors

object AppImplicits extends AppImplicits
