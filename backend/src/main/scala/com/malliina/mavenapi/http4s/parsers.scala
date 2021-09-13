package com.malliina.mavenapi.http4s

import cats.data.NonEmptyList
import com.malliina.mavenapi.{ArtifactId, GroupId, MavenQuery, ScalaVersion}
import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}
import scalatags.Text.all.s

object parsers:
  implicit val group: QueryParamDecoder[GroupId] = idQueryDecoder(GroupId.apply)
  implicit val artifact: QueryParamDecoder[ArtifactId] = idQueryDecoder(ArtifactId.apply)
  implicit val sv: QueryParamDecoder[ScalaVersion] = idQueryDecoder(ScalaVersion.apply)

  def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
    QueryParamDecoder.stringQueryParamDecoder.map(build)

  def parseOrDefault[T](q: Query, key: String, default: => T)(implicit
    dec: QueryParamDecoder[T]
  ) =
    parseOpt[T](q, key).getOrElse(Right(default))

  def parse[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
    parseOpt[T](q, key).getOrElse(
      Left(NonEmptyList(parseFailure(s"Query key not found: '$key'."), Nil))
    )

  def parseOpt[T](q: Query, key: String)(implicit
    dec: QueryParamDecoder[T]
  ): Option[Either[NonEmptyList[ParseFailure], T]] =
    q.params.get(key).map { g =>
      dec.decode(QueryParameterValue(g)).toEither
    }

  def parseOpt2[T](q: Query, key: String)(implicit
    dec: QueryParamDecoder[T]
  ): Either[NonEmptyList[ParseFailure], Option[T]] =
    q.params
      .get(key)
      .map { g =>
        dec.decode(QueryParameterValue(g)).toEither.map(Option.apply)
      }
      .getOrElse {
        Right(None)
      }

  def parseMavenQuery(q: Query): Either[NonEmptyList[ParseFailure], MavenQuery] = for
    g <- parseOpt2[GroupId](q, GroupId.key)
    a <- parseOpt2[ArtifactId](q, ArtifactId.key)
    sv <- parseOrDefault[ScalaVersion](q, ScalaVersion.key, ScalaVersion.scala213)
  yield MavenQuery(g, a, sv)

  def parseFailure(message: String) = ParseFailure(message, message)
