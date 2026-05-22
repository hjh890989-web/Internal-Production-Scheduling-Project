import { test, expect } from '@playwright/test'

/**
 * EP-E2E — 임의 시점 마스터 복원 UI (EP-19, REQ-FUNC-OC-014).
 *
 * <p>시나리오:
 * <ol>
 *   <li>{@code /audit/restore} 진입</li>
 *   <li>table select (vc_schedule / ex_schedule_candidate / order) + UUID 입력 + DatePicker</li>
 *   <li>입력 전 — Empty state 노출</li>
 *   <li>UUID 입력 → snapshot Card + timeline Divider 노출 (데이터 없으면 Empty)</li>
 *   <li>위험 방지 Alert 노출 — "복원은 별도 confirm 흐름"</li>
 * </ol>
 */
test.describe('EP-19 마스터 복원 UI', () => {
  test('진입 페이지 — table select + UUID input + DatePicker + 위험 방지 Alert', async ({ page }) => {
    await page.goto('/audit/restore')
    await page.waitForLoadState('networkidle')

    await expect(page.getByText('임의 시점 마스터 복원 (EP-19)')).toBeVisible()
    // 위험 방지 Alert
    await expect(page.getByText('audit forensic 조회')).toBeVisible()
    // 입력 위젯 3종
    await expect(page.getByRole('combobox')).toBeVisible()    // table select
    await expect(page.getByPlaceholder('row PK (UUID)')).toBeVisible()
    // DatePicker
    await expect(page.locator('.ant-picker').first()).toBeVisible()
  })

  test('row PK 미입력 — Empty state "row PK 를 입력하세요"', async ({ page }) => {
    await page.goto('/audit/restore')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('row PK 를 입력하세요')).toBeVisible()
  })

  test('row PK 입력 시 — snapshot Card + timeline Divider 노출 (데이터 0건 Empty 정상)', async ({ page }) => {
    await page.goto('/audit/restore')
    await page.waitForLoadState('networkidle')

    await page.getByPlaceholder('row PK (UUID)').fill('00000000-0000-0000-0000-000000000001')
    // snapshot Card title
    await expect(page.getByText('시점 snapshot (선택한 시각 기준)')).toBeVisible({ timeout: 5000 })
    // timeline Divider
    await expect(page.getByText('전체 timeline')).toBeVisible({ timeout: 5000 })
  })
})
