const ScalaJS = require('./scalajs.webpack.config');
const { merge } = require('webpack-merge');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const rootDir = path.resolve(__dirname, '../../../..');
const cssDir = path.resolve(rootDir, 'src/main/resources/css');

const WebApp = merge(ScalaJS, {
  entry: {
    styles: [path.resolve(cssDir, './mavenapi.js')],
    fonts: [path.resolve(cssDir, './fonts.js')]
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
        test: /\.less$/,
        use: [
          MiniCssExtractPlugin.loader,
          { loader: 'css-loader', options: { importLoaders: 1, url: true } },
          'postcss-loader',
          'less-loader'
        ]
      },
      {
        test: /\.(png|woff|woff2|eot|ttf|svg)$/,
        type: 'asset',
        generator: {
          filename: 'static/fonts/[name]-[hash][ext]'
        }
      },
      {
        test: /\.(png|svg)$/,
        type: 'asset/resource', // emits file
        include: [
          path.resolve(rootDir, 'src/main/resources')
        ],
        generator: {
          filename: 'img/[name][ext]'
        }
      }
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({filename: '[name].css'})
  ]
});

module.exports = WebApp;
