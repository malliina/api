package com.malliina.mvn.js

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Frontend:
  val bootstrapJs = Bootstrap
  val bootstrapCss = BootstrapCss
  val fontAwesomeCss = FontAwesomeCss

  def main(args: Array[String]): Unit =
    println("Hello")

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap/dist/css/bootstrap.min.css", JSImport.Namespace)
object BootstrapCss extends js.Object

@js.native
@JSImport("@fortawesome/fontawesome-free/css/all.min.css", JSImport.Namespace)
object FontAwesomeCss extends js.Object
