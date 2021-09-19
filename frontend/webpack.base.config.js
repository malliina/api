const ScalaJS = require('./scalajs.webpack.config');
const { merge } = require('webpack-merge');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const rootDir = path.resolve(__dirname, '../../../..');
const cssDir = path.resolve(rootDir, 'src/main/resources/css');

const WebApp = merge(ScalaJS, {
  entry: {
    styles: [path.resolve(cssDir, './mavenapi.js')],
    fonts: [path.resolve(cssDir, './fonts.js')],
    vendors: [path.resolve(cssDir, './vendors.js')]
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1, url: true } }
        ]
      },
      {
        test: /\.(png|woff|woff2|eot|ttf|svg)$/,
        use: [
          { loader: 'url-loader', options: { limit: 8192, name: 'static/fonts/[name]-[hash].[ext]' } }
        ]
      },
      {
        test: /\.(png|jpg|jpeg)$/,
        use: [
          { loader: 'file-loader', options: { name: '[folder]/[name].[ext]' } }
        ]
      },
      {
        test: /\.less$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1, url: false } },
          'postcss-loader',
          'less-loader'
        ]
      }
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({filename: '[name].css'})
  ]
});

module.exports = WebApp;
