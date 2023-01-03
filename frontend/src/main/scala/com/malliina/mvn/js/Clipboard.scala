package com.malliina.mvn.js

import org.scalajs.dom
import org.scalajs.dom.{HTMLInputElement, document}

object Clipboard:
  def copyToClipboard(text: String): Unit =
    // Create a "hidden" input
    val aux = dom.document.createElement("input").asInstanceOf[HTMLInputElement]
    // Assign it the value of the supplied parameter
    aux.setAttribute("value", text)
    // Append it to the body
    document.body.appendChild(aux)
    // Highlight its content
    aux.select()
    // Copy the highlighted text
    document.execCommand("copy")
    // Remove it from the body
    document.body.removeChild(aux)
