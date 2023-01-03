package com.malliina.mvn.js

import org.scalajs.dom
import org.scalajs.dom.Event

class Search:
  dom.document.body.getElementsByClassName("clipboard").foreach { copyable =>
    copyable.addEventListener(
      "click",
      (e: Event) => Clipboard.copyToClipboard(copyable.getAttribute("data-id"))
    )
  }
