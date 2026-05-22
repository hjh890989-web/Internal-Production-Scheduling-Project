import { LicenseManager, ModuleRegistry, AllEnterpriseModule } from 'ag-grid-enterprise'

/**
 * AG Grid Enterprise 라이센스 + 모듈 등록 — Sprint 5 EP-15 / EP-17.
 *
 * <p>1500 row × 30 col 가상 스크롤 + columnFilter + masterDetail + statusBar 필요.
 * Enterprise 라이센스는 사내 서버 한정 — Vite env {@code VITE_AG_GRID_LICENSE_KEY}.
 *
 * <p>DEV: 라이센스 미설정 시 워터마크 허용 (콘솔 경고만 출력).
 * STG/PROD: 빌드 시 환경 변수 주입 필수 (NFR-SEC-001 사내 한정).
 */

let initialized = false

export function initAgGridEnterprise(): void {
  if (initialized) return
  initialized = true

  ModuleRegistry.registerModules([AllEnterpriseModule])

  const licenseKey = import.meta.env.VITE_AG_GRID_LICENSE_KEY
  if (licenseKey) {
    LicenseManager.setLicenseKey(licenseKey)
  } else if (import.meta.env.MODE === 'production') {
    // 운영에서 라이센스 미주입은 에러 — 워터마크 출력 방지
    console.error(
      '[AG Grid] VITE_AG_GRID_LICENSE_KEY 미설정 (production). 라이센스 워터마크 노출 가능.',
    )
  }
}
