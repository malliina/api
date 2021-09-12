const BaseWebpack = require('./webpack.base.config');
const { merge } = require('webpack-merge');

module.exports = merge(BaseWebpack, {
  mode: 'production'
});
