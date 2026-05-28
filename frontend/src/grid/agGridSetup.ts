import { ModuleRegistry, AllCommunityModule, themeQuartz, type Theme } from 'ag-grid-community'

/**
 * AG Grid Community 모듈 + Theming API 등록 — Sprint 20 ST-COST-1 (Cost-Zero 정책).
 *
 * <p>1500 row × 30 col 가상 스크롤 + columnFilter + 정렬 + 페이지네이션 은
 * Community (MIT) 동일 지원. Enterprise 전용 statusBar / Range Selection /
 * Master-Detail / Set Filter 은 Sprint 20 폴백 (Set Filter → Text Filter).
 *
 * <p>AG Grid v33+ Theming API — CSS 파일 import 대신 `theme={agTheme}` prop 사용.
 * CSS + Theming 동시 사용은 error #239 충돌. 본 모듈이 `agTheme` 단일 export.
 *
 * <p>이력:
 * <ul>
 *   <li>Sprint 5 EP-15/17 v1.0 — AllEnterpriseModule + LicenseManager + CSS import (Enterprise $999~$1,599/dev/년)</li>
 *   <li>Sprint 20 ST-COST-1 v2.0 — Community AllCommunityModule + themeQuartz (MIT, $0). 사용자 10명 이내 운영 전제.</li>
 * </ul>
 *
 * <p>1년 뒤 부활 옵션: Single Dev License $999/년 구매 또는 TanStack Table 자체 구현 ~3 PD.
 */

let initialized = false

export function initAgGrid(): void {
  if (initialized) return
  initialized = true

  ModuleRegistry.registerModules([AllCommunityModule])
}

export const agTheme: Theme = themeQuartz
