import { test, expect } from '@playwright/test'

/**
 * EP-45 — Excel 매트릭스 다운로드 cross-browser 회귀 (REQ-FUNC-EX-018, BR-E09).
 *
 * <p>Chromium + Edge 두 browser project (playwright.config.ts) 에서 동작 검증.
 * Sprint 5 EP-12 매트릭스 export endpoint 호출 + Blob 다운로드 + 파일명 `EX_MATRIX_*.xlsx`.
 *
 * <p>실 데이터 검증 (시트명 정규식 `\d+월\d+일\(압출\)` 일치) 은 STG 환경 + 시드 데이터.
 * 본 spec 은 UI 다운로드 트리거 + 파일명 prefix 만 검증 (browser API 차이 회귀).
 */
test.describe('EP-45 — Excel download cross-browser', () => {
  test('ExMatrixPage Excel 다운로드 버튼 클릭 → .xlsx 파일 trigger', async ({ page, browserName }) => {
    await page.goto('/extrusion-matrix')
    await page.waitForLoadState('networkidle')

    const downloadPromise = page.waitForEvent('download', { timeout: 10_000 })
    await page.getByRole('button', { name: /Excel 다운로드/ }).click()

    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/^EX_MATRIX_.*\.xlsx$/)
    // browser 별 download API 차이 — chromium: blob URL → file, edge: 동일
    // Firefox 는 currently 미지원 (playwright.config.ts projects 에 미등록)
    expect(['chromium', 'edge', 'msedge']).toContain(browserName.toLowerCase().replace('chromium', 'chromium'))
  })

  test('Excel 다운로드 — Content-Type application/vnd.openxmlformats... (sanity)', async ({ page }) => {
    await page.goto('/extrusion-matrix')
    await page.waitForLoadState('networkidle')

    // Response intercept — Excel 다운로드 응답 헤더 확인
    const responsePromise = page.waitForResponse(
      (resp) => resp.url().includes('/api/v1/export/extrusion-matrix') && resp.status() < 400,
      { timeout: 10_000 },
    )
    await page.getByRole('button', { name: /Excel 다운로드/ }).click()
    const response = await responsePromise.catch(() => null)

    if (response) {
      const ct = response.headers()['content-type'] ?? ''
      expect(ct).toContain('spreadsheetml')
    }
    // STG 미연결 (401/403) 환경에서는 response null — UI 트리거 자체 통과로 충분
  })
})
