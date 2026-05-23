# PERF-002 — Sprint 7 종합 회귀 + Vite Bundle 분석 v1.1

**작성**: 2026-05-23 | **Sprint**: S7 (UI 마무리 + Phase 4 진입 직전) | **상위**: [PERF-001](PERF-001_NFR_Performance_Spec_v1.0.md) | **전판**: [v1.0](PERF-002_Bundle_Regression_Report_v1.0.md)

> v1.0 (backend 788 tests + frontend 54 vitest + entry 57.41kB) 이후 **BR-V12·V13 Planner UI 추가 + tooling cleanup 반영**.
> 진입 직전 baseline 재측정 — 회귀 0건.

---

## 1. v1.0 → v1.1 변경 요지

| 항목 | v1.0 | v1.1 | Δ |
|---|---:|---:|---|
| Frontend vitest | 54 | **58** | +4 (capacityOverflow.types 4) |
| Vite module 수 | 3273 | **3284** | +11 |
| Vite prod build 시간 | 14.54s | **14.79s** | +0.25s (미미) |
| Entry first paint | 57.41kB | **57.51kB** gzip | **+0.10kB** (라우터 lazy 추가만) |
| 신규 chunk | — | `CapacityQueuePage` 2.72kB gzip | 추가 lazy |
| VSCode 문제 탭 | 54 | **0** | -54 (markdownlint + cspell) |

→ 회귀 0 / 신규 chunk 의 first paint 영향 0.10kB 만 (DoD 200kB 여유 142.49kB 유지).

---

## 2. Backend 회귀 (788 tests / 0 failures, v1.0 대비 변동 없음)

CapacityOverflowController 는 v1.1 에서 추가했으나 controller layer IT 미작성 (Sprint 8+ 권장).
Service layer IT 5 (BrV12V13IT) + 단위 11 은 v1.0 시점 그대로 통과.

---

## 3. Frontend 회귀 v1.1

| 영역 | v1.0 | v1.1 |
|---|---|---|
| **vitest** | 54 tests / 0 failures | **58 tests / 0 failures** (+ capacityOverflow.types 4) |
| **ESLint** | 0 warning | 0 warning (`--max-warnings 0`) |
| **TypeScript** | 0 error | 0 error (`tsc -b` strict) |
| **Playwright spec** | 226 등록 | 226 등록 (변동 없음) |
| **production build** | ✅ 3273 module / 14.54s | ✅ **3284 module / 14.79s** |

---

## 4. Vite Bundle 상세 분석 v1.1

### 4.1 Chunk 크기 (production gzip 기준)

| 청크 | v1.0 gzip | v1.1 gzip | 비고 |
|---|---:|---:|---|
| `index-*.js` (entry) | 7.13kB | **7.23kB** | +0.10kB (route 1개 추가) |
| `react-vendor` | 20.92kB | 20.92kB | — |
| `i18n` | 16.41kB | 16.41kB | — (1 key 추가 미미) |
| `tanstack` | 11.55kB | 11.55kB | — |
| `client-*` (api) | 1.40kB | 1.40kB | — |
| **소계 — Entry first paint** | **57.41kB** | **57.51kB** | **+0.10kB** ≤ 200kB DoD ✅ |
| `HomePage` | 0.68kB | 0.68kB | — |
| `OrderImportPage` | 4.31kB | 4.31kB | — |
| `VcSimulationPage` | 2.31kB | 2.31kB | — |
| `ExMatrixPage` | 3.12kB | 3.13kB | — |
| `MasterRestorePage` | 1.64kB | 1.64kB | — |
| 🆕 `CapacityQueuePage` | — | **2.72kB** | 신규 page lazy (/vc/capacity-queue) |
| `antd-core` | 383.81kB | **392.35kB** | +8.54kB (Tabs + Statistic + Progress 신규 import) |
| `antd-icons` | 1.70kB | 1.70kB | — |
| `agGridSetup` | 653.33kB | **653.34kB** | 미미 |
| `stomp` | 21.53kB | 21.53kB | — |

> **antd-core +8.54kB** — Statistic·Progress·Tabs 컴포넌트가 신규 페이지에서 처음 사용된 결과.
> Lazy 청크이므로 첫 페이지 진입에 영향 없음. 이후 antd 페이지 진입 시 1회 fetch.

### 4.2 NFR-PER-005 (Entry first paint ≤ 200kB gzip)

```text
Sprint 5 baseline:    1.2MB ant-design 단일 청크 (FAIL)
Sprint 6 EP-46 fix:   ~50kB entry (PASS, 큰 폭 통과)
Sprint 7 v1.0:        57.41kB entry  ✅
Sprint 7 v1.1:        57.51kB entry  ✅  (+0.10kB only)
여유:                  142.49kB
```

### 4.3 Phase 4 베타 진입 권장사항 (v1.0 그대로 유지)

- **antd-core 392kB gzip** (v1.0 384 → v1.1 392, +8kB) — 첫 페이지 진입 후 antd 컴포넌트 로드 시 fetch. 베타 측정 시 페이지 진입 latency 모니터
- **agGridSetup 653kB gzip** — Enterprise 가 큼. AG Grid Charts 미사용 — chart module 제외 검토 (Phase 5+)
- **stomp 21.53kB** — 분리 양호, WebSocket 연결 시점 lazy
- 🆕 **CapacityQueuePage 2.72kB** — BR-V12·V13 UI, /vc/capacity-queue 진입 시만 fetch

### 4.4 Lighthouse Audit (v1.0 그대로 — Phase 4-A STG 진입 후)

본 환경 (개발자 PC) 에서 미실행. v1.0 §3.4 절차 동일 적용. 신규 page route 추가:

```bash
npx lighthouse http://localhost:4173/vc/capacity-queue \
    --preset=desktop --output=json --output-path=lighthouse-capacity-queue.json
```

---

## 5. 잠재 최적화 (Phase 5+ 검토 — v1.0 + v1.1 추가)

| 영역 | 현재 | 개선 후보 | 영향 |
|---|---|---|---|
| antd-core **392kB** | 단일 청크 lazy | `babel-plugin-import` 트리쉐이킹 | -50~100kB |
| agGridSetup 653kB | Enterprise 풀 모듈 | Charts/Reporting 모듈 제외 | -150kB |
| 페이지 lazy split | React.lazy + Suspense | 추가 dynamic import (Override 모달 등) | 마이크로 |
| Vite chunk warning limit | 700kB | dropdown (검사 강화) | 가독성 |
| SourceMap | 분리 (sourcemap: true) | PROD 별도 server 업로드 (Sentry) | -7~8MB total |
| 🆕 CapacityQueuePage Tabs lazy 분리 | 단일 chunk | Tab 별 dynamic import | -1kB (마이크로) |

---

## 6. Backend 성능 회귀 (v1.0 → v1.1, 변동 없음)

| 영역 | Sprint 6 | Sprint 7 v1.0 | Sprint 7 v1.1 |
|---|---|---|---|
| app 전수 회귀 | 249 tests | 254 tests | 254 tests (REST 추가, IT 미보강) |
| Modulith verify | 0 위반 | 0 위반 | 0 위반 |
| ArchUnit | 29 rule | 29 rule | 29 rule |
| Flyway | V001~V032 | V001~V033 | V001~V033 (변동 없음) |
| Modulith 모듈 | 9 | 9 | 9 |

---

## 7. Tooling — VSCode 문제 탭 노이즈 차단 (v1.1 신규)

| 파일 | 효과 |
|---|---|
| `.markdownlint.json` | MD013/024/026/029/033/034/036/040/041/046 11 룰 disable/relax — 한국어 마크다운 친화 |
| `.cspell.json` | 프로젝트 단어 ~100 (antd·Modulith·QueryDSL·hose·vulcanization·PDD/SAD/SRS 등) + Phase 1~5/`0.Pprompt/` 무시 |

**효과** — VSCode "문제" 탭 **54 → 0**. 향후 진짜 문제 (TS 잠재 이슈, 실제 오타) 식별 용이.

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | Sprint 7 종합 회귀 + Vite bundle 상세 + Lighthouse 절차 |
| 1.1 | 2026-05-23 | Claude Code | BR-V12·V13 UI 추가 반영 — vitest 54→58, entry +0.10kB, CapacityQueuePage 2.72kB chunk, antd-core +8.54kB. tooling .markdownlint+.cspell 추가 (문제 탭 54→0) |
