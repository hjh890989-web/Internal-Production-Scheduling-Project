import { test, expect } from '@playwright/test'
import scenarios from '../fixtures/drag-scenarios-100.json' assert { type: 'json' }

/**
 * TC-VC-004 — 100건 드래그 회귀 + ≤1초 차단 latency.
 *
 * <p>적합 50 + 비적합 50 → 적합은 슬롯 배치 성공, 비적합은 1초 이내 위반 모달 표시 + drag 원복.
 *
 * <p>실행 환경 — PLAYWRIGHT_BASE_URL (기본 localhost:5173). STG 환경 가동 + Keycloak SSO
 * 자동 로그인 (playwright.auth.json) 후 실행. Sprint Review 데모 + Jenkins CI stage.
 */
type Scenario = {
  case_id: string
  hose_id: string
  slot_position: string
  expected_eligible: boolean
}

const cases = scenarios as Scenario[]

test.describe('Slot drag guard — TC-VC-004', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
  })

  for (const sc of cases) {
    test(`${sc.case_id}: ${sc.hose_id} → ${sc.slot_position} (expected eligible=${sc.expected_eligible})`,
      async ({ page }) => {
        const hose = page.locator(`[data-testid="draggable-hose-${sc.hose_id}"]`)
        const slot = page.locator(`[data-testid="droppable-slot-${sc.slot_position}"]`)

        const t0 = Date.now()
        await hose.dragTo(slot)
        const elapsed = Date.now() - t0

        if (sc.expected_eligible) {
          // 적합 — 슬롯 내부에 hose 배치 (테스트 환경 의존 — Sprint Review 데모에서 detail 확정)
          await expect(slot).toContainText(sc.hose_id, { timeout: 2000 })
        } else {
          // 비적합 — ≤1초 위반 토스트 + 슬롯 미배치
          await expect(page.getByText(/적합하지 않습니다/)).toBeVisible({ timeout: 1000 })
          expect(elapsed).toBeLessThanOrEqualTo(1000)
          await expect(slot).not.toContainText(sc.hose_id)
        }
    })
  }
})
