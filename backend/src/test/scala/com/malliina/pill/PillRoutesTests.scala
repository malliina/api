package com.malliina.pill

import munit.FunSuite
import io.circe.syntax.EncoderOps
import com.malliina.pill.db.*

class PillRoutesTests extends FunSuite:
  test("json".ignore):
    val json = EnablePillNotifications(MobileOS.Apple, PushToken("jee")).asJson
    println(json)
