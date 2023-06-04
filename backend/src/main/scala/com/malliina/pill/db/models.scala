package com.malliina.pill.db

import com.malliina.values.{IdCompanion, WrappedId, WrappedString, StringEnumCompanion, StringCompanion}
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import java.time.Instant

case class PushToken(value: String) extends AnyVal with WrappedString
object PushToken extends StringCompanion[PushToken]

sealed abstract class MobileOS(val name: String) extends WrappedString:
  override def value: String = name

object MobileOS extends StringEnumCompanion[MobileOS]:
  override val all: Seq[MobileOS] = Seq(Apple, Android)
  override def write(os: MobileOS): String = os.name
  case object Apple extends MobileOS("apple")
  case object Android extends MobileOS("android")
  def unsafe(s: String): MobileOS =
    build(s).fold(e => throw new Exception(e.message), identity)

case class PillRowId(id: Long) extends WrappedId
object PillRowId extends IdCompanion[PillRowId]

case class PillRow(id: PillRowId, token: PushToken, os: MobileOS, added: Instant)

object PillRow:
  implicit val codec: Codec[PillRow] = deriveCodec[PillRow]
