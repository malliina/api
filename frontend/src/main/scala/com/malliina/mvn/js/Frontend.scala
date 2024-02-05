package com.malliina.mvn.js

import org.scalajs.dom
import org.scalajs.dom.document

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.Dynamic.literal

object Frontend:
  val bootstrapJs = Bootstrap
  val bootstrapCss = BootstrapCss
  val popperJs = Popper

  def main(args: Array[String]): Unit =
    println("Hello!")
    if has("search-page") then Search() else ()

  private def has(feature: String) = dom.document.body.classList.contains(feature)

@js.native
@JSImport("@popperjs/core", JSImport.Namespace)
object Popper extends js.Object

@js.native
trait PopoverOptions extends js.Object:
  def trigger: String

object PopoverOptions:
  def apply(trigger: String): PopoverOptions =
    literal(trigger = trigger).asInstanceOf[PopoverOptions]

  val click = apply("click")
  val focus = apply("focus")
  val manual = apply("manual")

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap", "Popover")
class Popover(e: dom.Element, options: PopoverOptions) extends js.Any:
  def hide(): Unit = js.native
  def show(): Unit = js.native
  def toggle(): Unit = js.native

@js.native
@JSImport("bootstrap/dist/css/bootstrap.min.css", JSImport.Namespace)
object BootstrapCss extends js.Object
