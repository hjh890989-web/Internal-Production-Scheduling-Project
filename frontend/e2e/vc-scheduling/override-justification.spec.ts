import { test, expect } from '@playwright/test'

/**
 * TK-04-3-2 + TK-04-3-3 — 강제 배치 사유 ≥10자 강제 검증 (REQ-FUNC-CO-010).
 *
 * <p>비적합 드래그 → ViolationModal 노출 → override 트리거 → 사유 입력 →
 *   - 10자 미만 → 차단
 *   - 10자 이상 → POST /api/v1/audit/override → 성공 toast → 슬롯 배치
 */
test.describe('Override justification — REQ-FUNC-CO-010', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
  })

  test('비적합 드래그 → 모달 → override 사유 미입력·짧음·정상 3 단계', async ({ page }) => {
    // 비적합 케이스 — REF-09 29673-2F900 의 LP_TOP 은 false
    await page.locator('[data-testid="draggable-hose-29673-2F900"]').dragTo(
      page.locator('[data-testid="droppable-slot-LP_TOP"]'),
    )

    // ViolationModal 노출
    await expect(page.getByText(/슬롯 적합성 위반/)).toBeVisible({ timeout: 1000 })
    await expect(page.getByTestId('violation-reason')).toBeVisible()

    // override 트리거
    await page.getByTestId('violation-override-trigger').click()
    await expect(page.getByTestId('override-form')).toBeVisible()

    // 사유 미입력 → submit 차단
    await page.getByRole('button', { name: '강제 배치 + audit 기록' }).click()
    await expect(page.getByText(/사유 입력 필수|10자 이상/)).toBeVisible()

    // 사유 10자 미만 → 차단
    await page.getByLabel('강제 배치 사유').fill('짧음')
    await page.getByRole('button', { name: '강제 배치 + audit 기록' }).click()
    await expect(page.getByText(/10자 이상/)).toBeVisible()

    // 사유 10자 이상 → audit 호출 → 성공
    await page.getByLabel('강제 배치 사유').fill('긴급 납기 대응 — 이수진 반장 사전 협의 완료')
    await page.getByRole('button', { name: '강제 배치 + audit 기록' }).click()
    await expect(page.getByText(/Override audit 기록 완료/)).toBeVisible({ timeout: 3000 })

    // 모달 닫힘 + 슬롯에 배치 확인
    await expect(page.getByText(/슬롯 적합성 위반/)).not.toBeVisible()
    await expect(page.locator('[data-testid="droppable-slot-LP_TOP"]'))
      .toContainText('29673-2F900')
  })

  test('취소 버튼 → 모달 닫힘 + 슬롯 미배치', async ({ page }) => {
    await page.locator('[data-testid="draggable-hose-29673-2F900"]').dragTo(
      page.locator('[data-testid="droppable-slot-LP_TOP"]'),
    )
    await expect(page.getByText(/슬롯 적합성 위반/)).toBeVisible({ timeout: 1000 })
    await page.getByTestId('violation-cancel').click()
    await expect(page.getByText(/슬롯 적합성 위반/)).not.toBeVisible()
    await expect(page.locator('[data-testid="droppable-slot-LP_TOP"]'))
      .not.toContainText('29673-2F900')
  })
})
