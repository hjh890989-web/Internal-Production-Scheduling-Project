import { test, expect, Page } from '@playwright/test'

/**
 * Sprint 24 S24-A ST-FB-1 — USER_MANUAL_v1.4 페르소나별 스크린샷 baseline.
 *
 * <p>4 role × 14 화면을 sample seed (V039 PLANNER / 99999-SAMPLE 호스) 로 캡처.
 * 결과는 docs/manual/screenshots/&lt;role&gt;/NN-name.png (fullPage).
 *
 * <p>실행 전제:
 * <ul>
 *   <li>frontend dev server (npm run dev) 또는 STG (PLAYWRIGHT_BASE_URL)</li>
 *   <li>backend with-infra + Keycloak sample realm + Flyway sample seed</li>
 *   <li>npx playwright install chromium</li>
 * </ul>
 *
 * <p>실행:
 * <pre>
 *   npx playwright test e2e/manual-capture/ --reporter=line
 * </pre>
 *
 * <p>BR 참조 — BR-X04 (Asia/Seoul), BR-X05 (dual-review), BR-X02 (audit), BR-V07 (D-0 lock).
 */

const SCREENSHOT_BASE = '../docs/manual/screenshots'

// Sample seed — Sprint 0 Flyway dev profile (시드 미존재 시 spec 만 보관, 실행은 carry-over)
const SAMPLE = {
  planner: { id: '20240101', pin: '1234' },     // ROLE_PLANNER (V039 group)
  stkUser: { id: '20240201', pin: '1234' },     // ROLE_STK_USER
  itOps:   { id: '20240301', pin: '1234' },     // ROLE_IT_OPS
  readOnly:{ id: '20240401', pin: '1234' },     // ROLE_READ_ONLY
  hoseId:  '99999-SAMPLE',                       // sample hose
} as const

async function login(page: Page, id: string, pin: string) {
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.locator('[data-testid="login-employee-id"]').fill(id)
  await page.locator('[data-testid="login-pin"]').fill(pin)
  await page.locator('[data-testid="login-submit"]').click()
  await page.waitForLoadState('networkidle')
}

async function shoot(page: Page, role: string, name: string) {
  await page.screenshot({
    path: `${SCREENSHOT_BASE}/${role}/${name}.png`,
    fullPage: true,
  })
}

test.use({
  locale: 'ko-KR',
  timezoneId: 'Asia/Seoul',
})

// ═══════════════════════════════════════════════════════════════════════════
// PLANNER — 6 화면 (계획 작성·확정·override)
// ═══════════════════════════════════════════════════════════════════════════
test.describe('PLANNER persona screenshots', () => {
  test('01-login — 사번/PIN 로그인 화면', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'planner', '01-login')
  })

  test('02-order-import — 수주 Excel 업로드 화면', async ({ page }) => {
    await login(page, SAMPLE.planner.id, SAMPLE.planner.pin)
    await page.goto('/orders/import')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'planner', '02-order-import')
  })

  test('03-diff — 수주 변경 diff (KD vs MES)', async ({ page }) => {
    await login(page, SAMPLE.planner.id, SAMPLE.planner.pin)
    await page.goto('/orders/diff')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'planner', '03-diff')
  })

  test('04-vc-simview — 성형 가류 Gantt 시뮬레이션 보드', async ({ page }) => {
    await login(page, SAMPLE.planner.id, SAMPLE.planner.pin)
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'planner', '04-vc-simview')
  })

  test('05-batch-confirm-modal — 일괄 확정 모달 (BR-X05 dual-review)', async ({ page }) => {
    await login(page, SAMPLE.planner.id, SAMPLE.planner.pin)
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
    await page.locator('[data-testid="batch-confirm-button"]').click()
    await expect(page.locator('[data-testid="confirm-modal"]')).toBeVisible()
    await shoot(page, 'planner', '05-batch-confirm-modal')
  })

  test('06-drawer — 호스 상세 drawer (제약·이력)', async ({ page }) => {
    await login(page, SAMPLE.planner.id, SAMPLE.planner.pin)
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
    await page.locator(`[data-testid="hose-row-${SAMPLE.hoseId}"]`).click()
    await expect(page.locator('[data-testid="hose-drawer"]')).toBeVisible()
    await shoot(page, 'planner', '06-drawer')
  })
})

// ═══════════════════════════════════════════════════════════════════════════
// STK_USER — 3 화면 (시뮬뷰·swap 제안)
// ═══════════════════════════════════════════════════════════════════════════
test.describe('STK_USER persona screenshots', () => {
  test('01-login-stk — STK 사번/PIN 로그인', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'stk-user', '01-login-stk')
  })

  test('02-vc-simview-read — 성형 보드 read-only 뷰', async ({ page }) => {
    await login(page, SAMPLE.stkUser.id, SAMPLE.stkUser.pin)
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'stk-user', '02-vc-simview-read')
  })

  test('03-swap-proposal — 호스 swap 제안 화면', async ({ page }) => {
    await login(page, SAMPLE.stkUser.id, SAMPLE.stkUser.pin)
    await page.goto('/orders/swap-proposal')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'stk-user', '03-swap-proposal')
  })
})

// ═══════════════════════════════════════════════════════════════════════════
// IT_OPS — 3 화면 (sample 가능 부분만, Grafana/폴백/감사 변조 = S24-B carry-over)
// ═══════════════════════════════════════════════════════════════════════════
test.describe('IT_OPS persona screenshots', () => {
  test('01-master-hub — 마스터 데이터 허브', async ({ page }) => {
    await login(page, SAMPLE.itOps.id, SAMPLE.itOps.pin)
    await page.goto('/master')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'it-ops', '01-master-hub')
  })

  test('02-vc-machine-admin — VC 기기 마스터 관리', async ({ page }) => {
    await login(page, SAMPLE.itOps.id, SAMPLE.itOps.pin)
    await page.goto('/master/vc-machines')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'it-ops', '02-vc-machine-admin')
  })

  test('03-holiday-admin — 휴일/근무캘린더 관리', async ({ page }) => {
    await login(page, SAMPLE.itOps.id, SAMPLE.itOps.pin)
    await page.goto('/master/holidays')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'it-ops', '03-holiday-admin')
  })
})

// ═══════════════════════════════════════════════════════════════════════════
// READ_ONLY — 2 화면 (감사·임원 조회)
// ═══════════════════════════════════════════════════════════════════════════
test.describe('READ_ONLY persona screenshots', () => {
  test('01-vc-simview-readonly — 성형 보드 readonly 뷰', async ({ page }) => {
    await login(page, SAMPLE.readOnly.id, SAMPLE.readOnly.pin)
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'read-only', '01-vc-simview-readonly')
  })

  test('02-audit-log-readonly — 감사 로그 readonly 조회 (BR-X02)', async ({ page }) => {
    await login(page, SAMPLE.readOnly.id, SAMPLE.readOnly.pin)
    await page.goto('/audit/log')
    await page.waitForLoadState('networkidle')
    await shoot(page, 'read-only', '02-audit-log-readonly')
  })
})
