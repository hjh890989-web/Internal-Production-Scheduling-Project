/**
 * Vite 5 config — Internal Production Scheduling SPA.
 *
 * proxy: DEV에서 BE Spring Boot (8080) 로 /api·/ws 자동 프록시.
 *        STG/PROD 는 NGINX 가 같은 역할 — Vite 는 정적 파일만 빌드 (`dist/`).
 * manualChunks: react·antd 분리 → 캐시 효율 + 초기 진입 ↓ (NFR-PER-005).
 */
declare const _default: import("vite").UserConfig;
export default _default;
