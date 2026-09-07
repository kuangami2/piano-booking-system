import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 用户端工程，开发端口 5173，/api 代理到后端 9090
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true
      },
      '/img': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true
      }
    }
  }
})
