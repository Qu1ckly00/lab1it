import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // усе, що починається з /api, піде на бекенд :8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // якщо на бекенді немає префікса /api, розкоментуйте рядок нижче:
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})