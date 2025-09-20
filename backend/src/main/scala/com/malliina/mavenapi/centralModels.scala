package com.malliina.mavenapi

import io.circe.Codec

case class ArtifactComponent(
  id: String,
  namespace: GroupId,
  name: String,
  version: String,
  publishedEpochMillis: Long
) derives Codec.AsObject

case class CentralResponse(components: Seq[ArtifactComponent], totalResultCount: Int)
  derives Codec.AsObject:
  def toResults = SearchResults(
    components.map(a => SearchResult(a.id, a.namespace, a.name, a.version, a.publishedEpochMillis))
  )
