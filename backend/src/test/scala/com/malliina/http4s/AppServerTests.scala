package com.malliina.http4s

import com.malliina.pill.{DisablePillNotifications, EnablePillNotifications, PillResponse}
import com.malliina.pill.db.MobileOS.Apple
import com.malliina.pill.db.{MobileOS, PillRow, PushToken}
import org.apache.commons.text.{CharacterPredicates, RandomStringGenerator}

class AppServerTests extends munit.CatsEffectSuite with ServerSuite:
  test("Enable and disable notification"):
    val srv = server()
    val pillUrl = srv.baseHttpUrl / "pill"
    val enableUrl = pillUrl / "enable"
    val disableUrl = pillUrl / "disable"
    val in = EnablePillNotifications(Apple, PushToken(randomString(6)))
    val client = srv.client
    for
      enabled <- client.postAs[EnablePillNotifications, PillRow](enableUrl, in)
      disableBody = DisablePillNotifications(enabled.token)
      disabled <- client.postAs[DisablePillNotifications, PillResponse](disableUrl, disableBody)
    yield assertEquals(disabled, PillResponse.done)

  private val generator: RandomStringGenerator =
    new RandomStringGenerator.Builder()
      .withinRange('a', 'z')
      .filteredBy(CharacterPredicates.LETTERS)
      .get()

  def randomString(length: Int) = generator.generate(length).toLowerCase
