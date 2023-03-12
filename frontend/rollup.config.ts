import resolve from "@rollup/plugin-node-resolve"
import commonjs from "@rollup/plugin-commonjs"
import terser from "@rollup/plugin-terser"
import url from "@rollup/plugin-url"
import {scalajs, production, outputDir} from "./target/scalajs.rollup.config.js"
import path from "path"
import extractcss from "./rollup-extract-css"
import type {RollupOptions} from "rollup"

const resourcesDir = "src/main/resources"
const cssDir = path.resolve(resourcesDir, "css")
const urlOptions = [
  {
    filter: "**/*.woff2",
    url: "inline"
  },
  {
    filter: "**/*.svg",
    url: "inline"
  },
  // maxSize is kilobytes
  {
    filter: "**/*",
    url: "inline",
    maxSize: 48,
    fallback: "copy",
    assetsPath: "assets", // this must be defined but can be whatever since it "cancels out" the "../" in the source files
    useHash: true,
    hashOptions: {
      append: true
    }
  }
]

const entryNames = "[name].js"

const css = () => extractcss({
  outDir: outputDir,
  minimize: production,
  urlOptions: urlOptions
})

const config: RollupOptions[] = [
  {
    input: scalajs.input,
    plugins: [
      css(),
      resolve({browser: true}),
      commonjs(),
      production && terser()
    ],
    output: {
      dir: outputDir,
      format: "iife",
      name: "version",
      entryFileNames: entryNames
    },
    context: "window"
  },
  {
    input: {
      fonts: path.resolve(cssDir, "fonts.js"),
      styles: path.resolve(cssDir, "mavenapi.js")
    },
    plugins: [
      url({
        limit: 0,
        fileName: production ? "[dirname][name].[hash][extname]" : "[dirname][name][extname]"
      }),
      css(),
      production && terser()
    ],
    output: {
      dir: outputDir,
      entryFileNames: entryNames
    }
  }
]

export default config
