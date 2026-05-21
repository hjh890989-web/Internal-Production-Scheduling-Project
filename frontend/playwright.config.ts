import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright 설정 — TK-04-3-3 (TC-VC-004 드래그 ≤1초 가드).
 *
 * <p>실 실행은 STG 환경 가동 + browser install 후 (`npx playwright install chromium`).
 * Sprint 2 baseline 은 spec 코드 형태로만 보관, Jenkins CI stage 활성 후 자동 실행.
 *
 * 실행:
 *   PLAYWRIGHT_BASE_URL=http://stg.intranet npx playwright test
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // 드래그 시나리오는 순차 (slot 점유 race 회피)
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['list']] : 'list',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    locale: 'ko-KR',
    timezoneId: 'Asia/Seoul',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'edge', use: { ...devices['Desktop Edge'], channel: 'msedge' } },
  ],
  // NOTE: webServer 는 STG 환경 활성 후 활성화 — 로컬 dev 자동 부팅은 Sprint 3+ 옵션
})
