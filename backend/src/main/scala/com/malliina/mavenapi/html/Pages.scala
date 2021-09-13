package com.malliina.mavenapi.html

import cats.Show
import com.malliina.http.FullUrl
import com.malliina.mavenapi.{AssetsSource, HashedAssetsSource, MavenDocument, MavenQuery, MavenSearchResults}
import org.http4s.Uri
import scalatags.Text.all.*
import scalatags.text.Builder

object Pages:
  def apply(): Pages =
    val absoluteScripts = Nil // FullUrl.build(LiveReload.script).toSeq
    new Pages(Nil, absoluteScripts, HashedAssetsSource)

class Pages(scripts: Seq[String], absoluteScripts: Seq[FullUrl], assets: AssetsSource):
  implicit def showAttrValue[T](implicit s: Show[T]): AttrValue[T] =
    (t: Builder, a: Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(s.show(v)))
  implicit val uriAttrValue: AttrValue[Uri] = new AttrValue[Uri]:
    override def apply(t: Builder, a: Attr, v: Uri): Unit =
      t.setAttr(a.name, Builder.GenericAttrValueSource(v.renderString))
  implicit val urlAttr: AttrValue[FullUrl] = genericAttr[FullUrl]

  def search(q: MavenQuery, results: Seq[MavenDocument]) = html(lang := "en")(
    head(
      meta(charset := "utf-8"),
      deviceWidthViewport,
      cssLink(at("vendors.css")),
      cssLink(at("fonts.css")),
      cssLink(at("styles.css")),
      absoluteScripts.map { url =>
        script(src.:=(url)(urlAttr), defer)
      }
    ),
    body(
      div(`class` := "search")(
        form(`class` := "maven-form", method := "POST", action := "/")(
          div(`class` := "mb-3")(
            label(`for` := "artifact-input", `class` := "form-label")("Artifact"),
            input(
              `type` := "text",
              `class` := "form-control",
              id := "artifact-input",
              name := "artifact",
              placeholder := "aws-sdk",
              value := q.artifact.map(_.id).getOrElse("")
            )
          ),
          div(`class` := "mb-3")(
            label(`for` := "group-input", `class` := "form-label")("Group"),
            input(
              `type` := "text",
              `class` := "form-control",
              id := "group-input",
              name := "group",
              placeholder := "com.amazon",
              value := q.group.map(_.id).getOrElse("")
            )
          ),
          button(`type` := "submit", `class` := "btn btn-primary")("Submit")
        ),
        if !q.isEmpty then
          if results.nonEmpty then
            table(`class` := "table mt-3")(
              thead(
                tr(
                  th(attr("scope") := "col")("Artifact"),
                  th(attr("scope") := "col")("Version")
                )
              ),
              tbody(
                results.map { (result: MavenDocument) =>
                  tr(
                    td(result.a),
                    td(result.v)
                  )
                }
              )
            )
          else p(`class` := "mt-3")(s"No results for ${q.describe}")
        else modifier()
      )
    )
  )

  def deferredJsPath(path: String) =
    script(`type` := "application/javascript", src := at(path), defer)

  def deviceWidthViewport =
    meta(name := "viewport", content := "width=device-width, initial-scale=1.0")

  def cssLink[V: AttrValue](url: V, more: Modifier*) =
    link(rel := "stylesheet", href := url, more)

  def at(path: String) = assets.at(path)
