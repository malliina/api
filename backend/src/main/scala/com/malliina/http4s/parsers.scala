package com.malliina.http4s

import cats.data.NonEmptyList
import com.malliina.mavenapi.{ArtifactId, GroupId, MavenQuery, ScalaVersion}
import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}

object parsers:
  given QueryParamDecoder[GroupId] = idQueryDecoder(GroupId.apply)
  given QueryParamDecoder[ArtifactId] = idQueryDecoder(ArtifactId.apply)
  given QueryParamDecoder[ScalaVersion] = idQueryDecoder(ScalaVersion.apply)

  private def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
    QueryParamDecoder.stringQueryParamDecoder.map(build)

  private def parseOpt2[T](q: Query, key: String)(implicit
    dec: QueryParamDecoder[T]
  ): Either[NonEmptyList[ParseFailure], Option[T]] =
    q.params
      .get(key)
      .map: g =>
        dec.decode(QueryParameterValue(g)).toEither.map(Option.apply)
      .getOrElse:
        Right(None)

  def parseMavenQuery(q: Query): Either[NonEmptyList[ParseFailure], MavenQuery] = for
    g <- parseOpt2[GroupId](q, GroupId.key)
    a <- parseOpt2[ArtifactId](q, ArtifactId.key)
    sv <- parseOpt2[ScalaVersion](q, ScalaVersion.key)
  yield MavenQuery(g, a, sv)

  def parseFailure(message: String) = ParseFailure(message, message)
