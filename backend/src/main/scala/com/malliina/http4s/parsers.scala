package com.malliina.http4s

import cats.data.NonEmptyList
import com.malliina.mavenapi.{ArtifactId, GroupId, MavenQuery, ScalaVersion}
import com.malliina.values.ErrorMessage
import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}

object parsers:
  given QueryParamDecoder[GroupId] = idQueryDecoder(GroupId.build)
  given QueryParamDecoder[ArtifactId] = idQueryDecoder(ArtifactId.build)
  given QueryParamDecoder[ScalaVersion] = idQueryDecoder(ScalaVersion.build)

  private def idQueryDecoder[T](build: String => Either[ErrorMessage, T]): QueryParamDecoder[T] =
    QueryParamDecoder.stringQueryParamDecoder.emap: s =>
      build(s).left.map(err => ParseFailure("Failed to parse query parameter.", err.message))

  private def parseOpt[T](q: Query, key: String)(using
    dec: QueryParamDecoder[T]
  ): Either[NonEmptyList[ParseFailure], Option[T]] =
    q.params
      .get(key)
      .map: g =>
        dec.decode(QueryParameterValue(g)).toEither.map(Option.apply)
      .getOrElse:
        Right(None)

  def parseMavenQuery(q: Query): Either[NonEmptyList[ParseFailure], MavenQuery] = for
    g <- parseOpt[GroupId](q, GroupId.key)
    a <- parseOpt[ArtifactId](q, ArtifactId.key)
    sv <- parseOpt[ScalaVersion](q, ScalaVersion.key)
  yield MavenQuery(g, a, sv)

  def parseFailure(message: String) = ParseFailure(message, message)
