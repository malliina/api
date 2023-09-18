package com.malliina.musicmeta

import com.malliina.http.FullUrl
import io.circe.Codec

case class CoverResult(cover_image: FullUrl) derives Codec.AsObject

case class CoverSearchResult(results: Seq[CoverResult]) derives Codec.AsObject
