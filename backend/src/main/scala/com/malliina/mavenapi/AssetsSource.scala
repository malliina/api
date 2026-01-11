package com.malliina.mavenapi

import com.malliina.http.FullUrl
import com.malliina.mavenapi.AssetsSource.prefix
import com.malliina.mvn.assets.HashedAssets
import org.http4s.Uri
import org.http4s.implicits.uri

trait AssetsSource:
  def at(file: String): Uri

object AssetsSource:
  val prefix = uri"/assets"
  def apply(isProd: Boolean): AssetsSource =
    if isProd then HashedAssetsSource
    else DirectAssets

object DirectAssets extends AssetsSource:
  override def at(file: String): Uri = prefix.addPath(file)

object HashedAssetsSource extends AssetsSource:
  override def at(file: String): Uri =
    val optimal = HashedAssets.assets.getOrElse(file, file)
    prefix.addPath(optimal)

class CDNAssets(cdnBaseUrl: FullUrl) extends AssetsSource:
  override def at(file: String): Uri =
    val optimal = HashedAssets.assets.getOrElse(file, file)
    val url = cdnBaseUrl / "assets" / optimal
    Uri.unsafeFromString(url.url)
