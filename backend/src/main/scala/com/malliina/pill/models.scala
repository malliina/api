package com.malliina.pill

import com.malliina.pill.db.{MobileOS, PushToken}
import io.circe.Codec

case class PillResponse(message: String) derives Codec.AsObject

case class EnablePillNotifications(os: MobileOS, token: PushToken) derives Codec.AsObject

case class DisablePillNotifications(token: PushToken) derives Codec.AsObject
