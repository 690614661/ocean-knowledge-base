const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 3000,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/doc.html': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/webjars': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/swagger-resources': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/v2': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
