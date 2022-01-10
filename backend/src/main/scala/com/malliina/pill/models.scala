package com.malliina.pill

import com.malliina.pill.db.{MobileOS, PushToken}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class PillResponse(message: String)

object PillResponse:
  implicit val codec: Codec[PillResponse] = deriveCodec[PillResponse]

case class EnablePillNotifications(os: MobileOS, token: PushToken)

object EnablePillNotifications:
  implicit val codec: Codec[EnablePillNotifications] = deriveCodec[EnablePillNotifications]

case class DisablePillNotifications(token: PushToken)

object DisablePillNotifications:
  implicit val codec: Codec[DisablePillNotifications] = deriveCodec[DisablePillNotifications]
