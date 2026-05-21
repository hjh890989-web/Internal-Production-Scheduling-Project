import { test, expect } from '@playwright/test'

/**
 * P4 페르소나 UAT — 단독 드래그 사용 가능 (US-02 AC-3 / NS-S03).
 *
 * <p>최민혁 대리 (P4) 가 P1 김정훈 주임 부재 시 단독으로 5건 배치 시연.
 * 매트릭스 자동 가드 → 비적합 슬롯 자동 회피 → BR-V11/V13 위반 0.
 *
 * <p>학습 시간 ≤ 30분 검증 — Phase 1.0 Stage 0 실 사용자 1h 세션 후 확정.
 */
test.describe('P4 페르소나 UAT — Sprint Review 데모', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/orders/vc-board')
    await page.waitForLoadState('networkidle')
  })

  test('5건 적합 슬롯 배치 — 위반 0', async ({ page }) => {
    // 후보 카드 5건 선정 (페이지에 렌더된 첫 5건)
    const candidates = page.locator('[data-testid^="draggable-hose-"]')
    const count = Math.min(5, await candidates.count())
    expect(count).toBeGreaterThan(0)

    let placedCount = 0
    for (let i = 0; i < count; i++) {
      const hose = candidates.nth(i)
      const hoseId = await hose.getAttribute('data-testid').then(s => s!.replace('draggable-hose-', ''))

      // 적합 슬롯 탐색 (LP/IC 7 슬롯 중 첫 매칭)
      const allSlots = ['LP_UPMID', 'LP_LOWMID', 'IC_TOP', 'IC_MID', 'IC_BOT', 'LP_TOP', 'LP_BOT']
      let placed = false
      for (const slot of allSlots) {
        const target = page.locator(`[data-testid="droppable-slot-${slot}"]`)
        if ((await target.getAttribute('data-eligible')) === 'false') continue
        await hose.dragTo(target)
        if (await target.textContent().then(t => t?.includes(hoseId))) {
          placed = true
          placedCount++
          break
        }
      }
      expect(placed, `${hoseId} 배치 슬롯 미발견`).toBeTruthy()
    }
    expect(placedCount).toEqual(count)
  })

  test('한국어 UI 100% (NFR-USA-003) — 핵심 텍스트 한국어 노출', async ({ page }) => {
    await expect(page.getByText('후보 품번')).toBeVisible()
    await expect(page.getByText('가류기 슬롯')).toBeVisible()
    // matrix v{N} 라벨
    await expect(page.locator('text=/matrix v\\d+/')).toBeVisible()
  })
})
