package com.malliina.mavenapi.html

import cats.Show
import com.malliina.http.FullUrl
import com.malliina.live.LiveReload
import com.malliina.mavenapi.html.Pages.{scope, urlAttr}
import com.malliina.mavenapi.html.Pages.given
import com.malliina.mavenapi.{AssetsSource, BuildInfo, MavenDocument, MavenQuery}
import com.malliina.mvn.assets.{FileAssets, HashedAssets}
import org.http4s.Uri
import scalatags.Text.all.*
import scalatags.text.Builder

object Pages:
  private val scope = attr("scope")

  def default(isProd: Boolean = BuildInfo.isProd): Pages =
    val isLiveReloadEnabled = !LiveReload.script.contains("12345")
    val absoluteScripts =
      if isLiveReloadEnabled then FullUrl.build(LiveReload.script).toSeq else Nil
    val css = Seq(FileAssets.frontend_css, FileAssets.fonts_css, FileAssets.styles_css)
    val assetsFinder = AssetsSource(isProd)
    val appScripts = Seq(FileAssets.frontend_js)
    Pages(appScripts, absoluteScripts, css, assetsFinder)

  given showAttrValue[T](using s: Show[T]): AttrValue[T] =
    (t: Builder, a: Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(s.show(v)))
  given AttrValue[Uri] = (t: Builder, a: Attr, v: Uri) =>
    t.setAttr(a.name, Builder.GenericAttrValueSource(v.renderString))
  given urlAttr: AttrValue[FullUrl] = genericAttr[FullUrl]

class Pages(
  scripts: Seq[String],
  absoluteScripts: Seq[FullUrl],
  cssFiles: Seq[String],
  assets: AssetsSource
):
  def search(q: MavenQuery, results: Seq[MavenDocument]) =
    html(lang := "en")(
      head(
        meta(charset := "utf-8"),
        tag("title")("Search artifacts"),
        deviceWidthViewport,
        link(
          rel := "shortcut icon",
          `type` := "image/png",
          href := inlineOrAsset(FileAssets.img.jag_16x16_png)
        ),
        cssFiles.map(file => cssLink(at(file))),
        absoluteScripts.map(url => script(src.:=(url)(urlAttr), defer)),
        scripts.map: path =>
          deferredJsPath(path)
      ),
      body(`class` := "search-page")(
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
                    th(scope := "col")("Artifact"),
                    th(scope := "col")("Version"),
                    th(scope := "col")("sbt"),
                    th(scope := "col")("Copy")
                  )
                ),
                tbody(
                  results.map: (result: MavenDocument) =>
                    tr(
                      td(result.a),
                      td(result.v),
                      td(code(result.sbt)),
                      td(div(`class` := "clipboard", data("id") := result.sbt))
                    )
                )
              )
            else p(`class` := "mt-3")(s"No results for ${q.describe}")
          else modifier()
        )
      )
    )

  private def deferredJsPath(path: String) =
    script(`type` := "application/javascript", src := at(path), defer)
  private def deviceWidthViewport =
    meta(name := "viewport", content := "width=device-width, initial-scale=1.0")
  private def cssLink[V: AttrValue](url: V, more: Modifier*) =
    link(rel := "stylesheet", href := url, more)
  private def inlineOrAsset(path: String) =
    HashedAssets.dataUris.getOrElse(path, at(path).toString)
  private def at(path: String) = assets.at(path)
