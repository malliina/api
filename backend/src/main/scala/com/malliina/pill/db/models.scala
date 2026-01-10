package com.malliina.pill.db

import com.malliina.values.{ErrorMessage, StringEnumCompanion, ValidatedLong, ValidatedString, WrappedString}
import io.circe.Codec

import java.time.Instant

opaque type PushToken = String
object PushToken extends ValidatedString[PushToken]:
  override def build(input: String): Either[ErrorMessage, PushToken] =
    Right(input)
  override def write(t: PushToken): String = t

enum MobileOS(val name: String) extends WrappedString:
  override def value: String = name
  case Apple extends MobileOS("apple")
  case Android extends MobileOS("android")

object MobileOS extends StringEnumCompanion[MobileOS]:
  override val all: Seq[MobileOS] = Seq(Apple, Android)
  override def write(os: MobileOS): String = os.name

opaque type PillRowId = Long
object PillRowId extends ValidatedLong[PillRowId]:
  override def build(input: Long): Either[ErrorMessage, PillRowId] =
    Right(input)
  override def write(t: PillRowId): Long = t

case class PillRow(id: PillRowId, token: PushToken, os: MobileOS, added: Instant)
  derives Codec.AsObject
