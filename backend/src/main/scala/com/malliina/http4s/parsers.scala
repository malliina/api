package com.malliina.http4s

import com.malliina.http.Errors
import com.malliina.mavenapi.{ArtifactId, GroupId, MavenQuery, ScalaVersion}
import org.http4s.{ParseFailure, Query, QueryParamDecoder}

object parsers:
  given QueryParamDecoder[GroupId] = QueryParsers.decoder(GroupId.build)
  given QueryParamDecoder[ArtifactId] = QueryParsers.decoder(ArtifactId.build)
  given QueryParamDecoder[ScalaVersion] = QueryParsers.decoder(ScalaVersion.build)

  def parseMavenQuery(q: Query): Either[Errors, MavenQuery] = for
    g <- QueryParsers.parseOptE[GroupId](q, GroupId.key)
    a <- QueryParsers.parseOptE[ArtifactId](q, ArtifactId.key)
    sv <- QueryParsers.parseOptE[ScalaVersion](q, ScalaVersion.key)
  yield MavenQuery(g, a, sv)

  def parseFailure(message: String) = ParseFailure(message, message)
