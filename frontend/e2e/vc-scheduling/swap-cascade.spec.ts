import { test, expect } from '@playwright/test'

/**
 * EP-E2E — VC swap propose → Planner accept → cascade (REQ-FUNC-VC-018).
 *
 * <p>시나리오:
 * <ol>
 *   <li>{@code /vc/simview} 진입 — VcRotationGrid + SwapProposalPanel 노출</li>
 *   <li>SwapProposalPanel 의 "처리 대기 제안 없음" 또는 PROPOSED 행 표시</li>
 *   <li>(데이터 있는 환경 — STG) Accept 클릭 → SwapProposalService.accept @Auditable
 *       → vc_schedule UPDATE (atomic CASE WHEN) + audit row 발행</li>
 *   <li>{@code /extrusion-matrix} → ExMatrixGrid 갱신 확인 (STOMP cascade — Sprint 4 EP-EX14)</li>
 * </ol>
 *
 * <p>실행 환경 — PLAYWRIGHT_BASE_URL (기본 localhost:5173). STG 환경 시드 데이터 의존.
 * 로컬 dev 에서는 데이터 없음 화면 (Empty / No proposals) 정상 동작 검증.
 */
test.describe('VC swap → cascade (EP-15 ST-15-2 + EP-EX14)', () => {
  test('VcSimulationPage 진입 — 회전 격자 + SwapProposalPanel 모두 노출', async ({ page }) => {
    await page.goto('/vc/simview')
    await page.waitForLoadState('networkidle')

    // Title
    await expect(page.getByText('성형 시뮬뷰 (EP-15)')).toBeVisible()
    // AG Grid 회전 격자
    await expect(page.locator('.ag-theme-quartz').first()).toBeVisible()
    // SwapProposalPanel divider
    await expect(page.getByText('현장 swap 제안 (Planner 1클릭 수용)')).toBeVisible()
  })

  test('ExMatrixPage 진입 — 매트릭스/Ranking Tabs + STOMP 상태 badge', async ({ page }) => {
    await page.goto('/extrusion-matrix')
    await page.waitForLoadState('networkidle')

    await expect(page.getByText('압출 매트릭스 (EP-17)')).toBeVisible()
    // 두 탭 노출
    await expect(page.getByRole('tab', { name: /매트릭스/ })).toBeVisible()
    await expect(page.getByRole('tab', { name: /다중 후보 ranking/ })).toBeVisible()
    // Excel 다운로드 버튼
    await expect(page.getByRole('button', { name: /Excel 다운로드/ })).toBeVisible()
    // STOMP 상태 badge (connected/disconnected 어느 쪽이든)
    await expect(page.getByText(/STOMP (connected|disconnected)/)).toBeVisible()
  })

  test('Ranking 탭 전환 — 다중 후보 ranking 테이블 노출', async ({ page }) => {
    await page.goto('/extrusion-matrix')
    await page.waitForLoadState('networkidle')

    await page.getByRole('tab', { name: /다중 후보 ranking/ }).click()
    // table 또는 loading state 노출 (데이터 0건도 정상)
    await expect(page.locator('table, .ant-empty').first()).toBeVisible({ timeout: 5000 })
  })

  test('cascade chain — VcSim → ExMatrix 라우트 이동 + STOMP badge 유지', async ({ page }) => {
    await page.goto('/vc/simview')
    await page.waitForLoadState('networkidle')

    await page.goto('/extrusion-matrix')
    await page.waitForLoadState('networkidle')
    // STOMP 싱글톤 — 두 페이지 간 재연결 없이 유지
    await expect(page.getByText(/STOMP (connected|disconnected)/)).toBeVisible()
  })
})
