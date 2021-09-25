package com.malliina.pill.db

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum MobileOS(name: String):
  case Apple extends MobileOS("apple")
  case Android extends MobileOS("android")

case class PillInput(device: String)

object PillInput:
  implicit val codec: Codec[PillInput] = deriveCodec[PillInput]

case class PillRow(id: Long, device: String, os: MobileOS)

object PillRow:
  implicit val codec: Codec[PillRow] = deriveCodec[PillRow]
