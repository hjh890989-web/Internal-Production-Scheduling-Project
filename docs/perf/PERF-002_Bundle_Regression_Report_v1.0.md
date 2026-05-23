# PERF-002 — Sprint 7 종합 회귀 + Vite Bundle 분석 v1.0

**작성**: 2026-05-23 | **Sprint**: S7 (마감 진입) | **상위**: [PERF-001](PERF-001_NFR_Performance_Spec_v1.0.md)

> Sprint 7 BR-V12·V13 추가 후 종합 회귀 + Vite production bundle 상세 분석.
> Phase 4 베타 진입 전 baseline metric 확정.

---

## 1. Backend 회귀 (788 tests / 0 failures / 0 errors)

| 모듈 | tests | failure | 비고 |
|---|---:|---:|---|
| common | 0 | 0 | (BR enum + record only) |
| master | 73 | 0 | KdOrder 6 + 기존 (V033 신규 6 포함) |
| order | 200 | 0 | Excel parser + Diff + FolderWatcher |
| notify | 22 | 0 | Resilience4j Kakao + STOMP |
| audit | 0 | 0 | (trigger 만, IT 는 app 에 통합) |
| vc | 153 | 0 | Allocator + 5 rule + CapacityOverflowQueue 5 신규 |
| ex | 86 | 0 | Yield + Grouping + Gate + Ranking |
| **app** | **254** | **0** | 통합 IT — BrV12V13IT 5 신규 |
| **합계** | **788** | **0** | 환경 0 error |

**누적 Sprint 0~7** — 단위 + IT 전수 통과.

---

## 2. Frontend 회귀

| 영역 | 결과 |
|---|---|
| **vitest** | 54 tests / 0 failures (i18n + types + components + pivot) |
| **ESLint** | 0 warning (`--max-warnings 0`) |
| **TypeScript** | 0 error (`tsc -b` strict + baseUrl 제거 후 TS 7.0 호환) |
| **Playwright spec** | 226 등록 (6 spec × 2 browser project — Chromium + Edge) |
| **production build** | ✅ 3273 module / 14.54s |

---

## 3. Vite Bundle 상세 분석

### 3.1 Chunk 크기 (production gzip 기준)

| 청크 | gzip | 분류 | 진입 시점 |
|---|---:|---|---|
| `index-*.js` (entry) | 7.13kB | Entry | 첫 페이지 |
| `react-vendor` | 20.92kB | vendor | 첫 페이지 |
| `i18n` | 16.41kB | vendor | 첫 페이지 |
| `tanstack` | 11.55kB | vendor | 첫 페이지 |
| `client-*` (api) | 1.40kB | app | 첫 페이지 |
| **소계 — Entry first paint** | **57.41kB** | | ≤ 200kB DoD ✅ |
| `HomePage` | 0.68kB | page lazy | /home |
| `OrderImportPage` | 4.31kB | page lazy | /orders/import |
| `VcSimulationPage` | 2.31kB | page lazy | /vc/simview |
| `ExMatrixPage` | 3.12kB | page lazy | /extrusion-matrix |
| `MasterRestorePage` | 1.64kB | page lazy | /audit/restore |
| `antd-core` | **383.81kB** | UI lib lazy | antd 첫 사용 페이지 |
| `antd-icons` | 1.70kB | UI lib lazy | icon 첫 사용 |
| `agGridSetup` | **653.33kB** | UI lib lazy | /vc/simview 또는 /extrusion-matrix |
| `stomp` | 21.53kB | feature lazy | SockJS 연결 시 |
| **agGridSetup CSS** | 44.98kB | UI lib lazy | AG Grid 페이지 |

### 3.2 NFR-PER-005 (Entry first paint ≤ 200kB gzip)

```
Sprint 5 baseline:    1.2MB ant-design 단일 청크 (FAIL)
Sprint 6 EP-46 fix:   ~50kB entry (PASS, 큰 폭 통과)
Sprint 7 현재:         57.41kB entry  ✅
여유:                  142.59kB
```

### 3.3 Phase 4 베타 진입 권장사항

- **antd-core 384kB gzip** — 첫 페이지 진입 후 antd 컴포넌트 로드 시 fetch. 베타 측정 시 페이지 진입 latency 모니터 필요
- **agGridSetup 653kB gzip** — Enterprise 가 큼. AG Grid Charts 미사용 — chart module 제외 검토 (Phase 5+)
- **stomp 21.53kB** — 분리 양호, WebSocket 연결 시점 lazy

### 3.4 Lighthouse Audit (Phase 4-A STG 진입 후)

본 환경 (개발자 PC) 에서는 미실행. STG 부팅 후 다음 절차:

```bash
# 1. STG dist build + serve
cd frontend && npm run build && npx serve dist -p 4173

# 2. Lighthouse CLI
npx lighthouse http://localhost:4173/extrusion-matrix \
    --preset=desktop --output=json --output-path=lighthouse-ex-matrix.json

# 3. 측정 항목 (NFR-PER-006)
#    - First Contentful Paint (FCP) ≤ 1.5s
#    - Largest Contentful Paint (LCP) ≤ 2.5s
#    - Time to Interactive (TTI) ≤ 3.5s
#    - Cumulative Layout Shift (CLS) ≤ 0.1
#    - AG Grid 1500 row first render ≤ 500ms (Sprint 5 DoD)
```

---

## 4. 잠재 최적화 (Phase 5+ 검토)

| 영역 | 현재 | 개선 후보 | 영향 |
|---|---|---|---|
| antd-core 384kB | 단일 청크 lazy | `babel-plugin-import` 트리쉐이킹 | -50~100kB |
| agGridSetup 653kB | Enterprise 풀 모듈 | Charts/Reporting 모듈 제외 | -150kB |
| 페이지 lazy split | React.lazy + Suspense | 추가 dynamic import (Override 모달 등) | 마이크로 |
| Vite chunk warning limit | 700kB | dropdown (검사 강화) | 가독성 |
| SourceMap | 분리 (sourcemap: true) | PROD 별도 server 업로드 (Sentry) | -7~8MB total |

---

## 5. Backend 성능 회귀

| 영역 | Sprint 6 | Sprint 7 | 변화 |
|---|---|---|---|
| app 전수 회귀 | 249 tests | 254 tests | +5 (BrV12V13IT) |
| Modulith verify | 0 위반 | 0 위반 | — |
| ArchUnit | 29 rule | 29 rule | — |
| Flyway | V001~V032 | V001~V033 | +1 (PRODUCT_PRIORITY + KD_ORDER) |
| Modulith 모듈 | 9 | 9 | (BR-V12·V13 은 vc 내부 신규 패키지) |

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | Sprint 7 종합 회귀 + Vite bundle 상세 + Lighthouse 절차 |
