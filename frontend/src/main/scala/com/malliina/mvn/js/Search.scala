package com.malliina.mvn.js

import org.scalajs.dom.{Event, document}

class Search:
  document.body
    .getElementsByClassName("clipboard")
    .foreach: copyable =>
      copyable.addEventListener(
        "click",
        (e: Event) => Clipboard.copyToClipboard(copyable.getAttribute("data-id"))
      )

//  document
//    .querySelectorAll("[data-bs-toggle='popover']")
//    .map: e =>
//      Popover(e, PopoverOptions.focus)
