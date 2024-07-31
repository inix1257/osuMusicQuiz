const { defineConfig } = require('@vue/cli-service')

const host = "localhost"; //back-end host
const port = "8080"; //back-end port

// const host = "localhost"; //back-end host
// const port = "8080"; //back-end port
module.exports = defineConfig({
  transpileDependencies: true,
  filenameHashing: false,
  productionSourceMap: true,
  css: { sourceMap: true },
  outputDir: "../src/main/resources/static", //webpack build 시 결과물 위치

  configureWebpack: (config) => {
    config.output.filename = "js/[name].[hash].js";
    config.output.chunkFilename = "js/[name].[hash].js";
  },

  devServer: {
    historyApiFallback: true,
    hot: true,
    proxy: {
      '/': {
        target: `http://${host}:${port}`,
        changeOrigin: true,
      },

      '/ws/': {
        target: `ws://${host}:${port}`,
        changeOrigin: true,
        ws: true,
      },

      '/ws-stomp/': {
        target: `http://${host}:${port}/ws-stomp`,
        changeOrigin: true,
      },

      'https://osu.ppy.sh/': {
        target: 'https://osu.ppy.sh/',
        changeOrigin: true,
      }
    }
  }
})