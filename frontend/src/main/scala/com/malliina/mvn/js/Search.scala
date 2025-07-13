package com.malliina.mvn.js

import org.scalajs.dom.{Event, document}

class Search:
  val popovers = document
    .querySelectorAll("[data-bs-toggle='popover']")
    .map: e =>
      new Popover(e, PopoverOptions.click)
  println(s"Hi, got ${popovers.size} popover(s).")

  document.body
    .getElementsByClassName("clipboard")
    .foreach: copyable =>
      copyable.addEventListener(
        "click",
        (_: Event) => Clipboard.copyToClipboard(copyable.getAttribute("data-id"))
      )
