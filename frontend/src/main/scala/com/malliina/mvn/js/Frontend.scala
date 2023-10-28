package com.malliina.mvn.js

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Frontend:
  val bootstrapJs = Bootstrap
  val bootstrapCss = BootstrapCss

  def main(args: Array[String]): Unit =
    println("Hello!!!")
    if has("search-page") then Search() else ()

  def has(feature: String) = dom.document.body.classList.contains(feature)

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap/dist/css/bootstrap.min.css", JSImport.Namespace)
object BootstrapCss extends js.Object
