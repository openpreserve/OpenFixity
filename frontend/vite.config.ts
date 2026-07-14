import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { readFileSync } from 'fs'

const packageJson = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8')) as {
  version?: string
}

// https://vite.dev/config/
export default defineConfig({
  base: '/app/',  // React app will be served at /app/ path
  // Target an older engine so the bundle also runs in the JavaFX WebView (desktop app),
  // whose WebKit is ~Safari 14-era. Still fine for modern browsers.
  build: {
    target: 'safari13',
  },
  define: {
    __APP_VERSION__: JSON.stringify(packageJson.version ?? '0.0.0'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
  },
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/scans': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
