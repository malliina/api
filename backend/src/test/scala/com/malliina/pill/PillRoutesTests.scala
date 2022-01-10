package com.malliina.pill

import com.malliina.pill.EnablePillNotifications
import munit.FunSuite
import io.circe.syntax.EncoderOps
import com.malliina.pill.db.*

class PillRoutesTests extends FunSuite:
  test("json") {
    val json = EnablePillNotifications(MobileOS.Apple, PushToken("jee")).asJson
    println(json)
  }
