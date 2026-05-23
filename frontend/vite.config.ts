/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

/**
 * Vite 5 config — Internal Production Scheduling SPA.
 *
 * proxy: DEV에서 BE Spring Boot (8080) 로 /api·/ws 자동 프록시.
 *        STG/PROD 는 NGINX 가 같은 역할 — Vite 는 정적 파일만 빌드 (`dist/`).
 * manualChunks: react·antd 분리 → 캐시 효율 + 초기 진입 ↓ (NFR-PER-005).
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: '127.0.0.1',          // DEV: localhost only (NFR-SEC-001)
    proxy: {
      '/api':  { target: 'http://localhost:8080', changeOrigin: true },
      '/ws':   { target: 'ws://localhost:8080',   ws: true },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    target: 'es2022',
    // EP-46 — Sprint 5 ant-design 단일 청크 1.2MB → 세분화 + AG Grid/STOMP 별도 chunk
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor':  ['react', 'react-dom', 'react-router-dom'],
          'antd-core':     ['antd'],                            // 단일 import 분리
          'antd-icons':    ['@ant-design/icons'],
          'tanstack':      ['@tanstack/react-query', '@tanstack/react-query-devtools'],
          'i18n':          ['i18next', 'react-i18next'],
          'dayjs':         ['dayjs'],
          'dnd-kit':       ['@dnd-kit/core', '@dnd-kit/sortable', '@dnd-kit/utilities'],
          'stomp':         ['@stomp/stompjs', 'sockjs-client'],
          // ag-grid 는 자동 분리 (별도 chunk — agGridSetup.ts import 시점)
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test-setup.ts',
    // TK-04-3-3 — Playwright e2e/ 디렉터리는 별도 runner (npx playwright test) 로 실행
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
})
