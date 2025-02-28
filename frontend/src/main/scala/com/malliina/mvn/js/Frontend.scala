package com.malliina.mvn.js

import org.scalajs.dom

import scala.annotation.unused
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
  def trigger: String = js.native
  def delay: Int = js.native
  def container: String = js.native
  def sanitize: Boolean = js.native

object PopoverOptions:
  def apply(
    trigger: String,
    delay: Int = 1,
    container: String = "body",
    sanitize: Boolean = false
  ): PopoverOptions =
    literal(trigger = trigger, delay = delay, container = container, sanitize = sanitize)
      .asInstanceOf[PopoverOptions]

  val click = apply("click")
  val focus = apply("focus")
  val manual = apply("manual")

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap", "Popover")
class Popover(@unused e: dom.Element, @unused options: PopoverOptions) extends js.Any:
  def hide(): Unit = js.native
  def show(): Unit = js.native
  def toggle(): Unit = js.native

@js.native
@JSImport("bootstrap/dist/css/bootstrap.min.css", JSImport.Namespace)
object BootstrapCss extends js.Object
