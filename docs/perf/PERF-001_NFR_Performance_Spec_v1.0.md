# PERF-001 — 성능 NFR 명세 v1.0 (EP-40)

**Sprint**: S6 | **Epic**: EP-40 성능 NFR | **상위 참조**: SRS REQ-NF-PER-001~006, NS-S04·S09

> 1500 row × 30 col 매트릭스 + UI 첫 렌더 + k6 부하 동시 100 사용자 5분 — 본 문서는
> Sprint 6 EP-40 의 측정 대상 + 임계 + 실행 절차를 정리.

---

## 1. NFR 측정 대상

| ID | 영역 | 측정 위치 | 목표 (p95) | 출처 |
|---|---|---|---|---|
| **REQ-NF-PER-001** | EX matrix API | `GET /schedule/ex/matrix` | ≤ 800ms | SRS v1.5 |
| **REQ-NF-PER-002** | Ranking API | `GET /schedule/ex/candidates/ranking` | ≤ 1,200ms | SRS v1.5 |
| **REQ-NF-PER-003** | VC slots API | `GET /schedule/vc/slots` | ≤ 800ms | SRS v1.5 |
| **REQ-NF-PER-004** | STOMP PUSH | `/topic/extrusion-updates` | ≤ 2,000ms | EP-EX14 (Sprint 4 ✅) |
| **REQ-NF-PER-005** | UI 첫 진입 entry bundle | Lighthouse Network | ≤ 200kB gzip | SRS v1.5 |
| **REQ-NF-PER-006** | AG Grid 1500 row 첫 렌더 | Lighthouse FCP | ≤ 500ms | Sprint 5 EntryPlan §6 |
| **REQ-NF-PER-007** | 동시 100 사용자 5분 | k6 ramping-vus | error rate < 1% | NFR baseline |

---

## 2. k6 부하 시나리오 — `infra/k6/matrix-1500-row.js`

```
ramp_up (총 5분):
  0:00~0:30  →  20 VUs (warm-up)
  0:30~1:30  →  50 VUs
  1:30~3:30  → 100 VUs (peak)
  3:30~4:30  → 100 VUs (hold)
  4:30~5:00  →   0 VUs (ramp down)
```

**실행 (STG 환경)**:
```bash
export K6_BASE_URL=http://stg.intranet:8080
export K6_JWT=$(curl -s ...login... | jq -r .token)
k6 run infra/k6/matrix-1500-row.js
```

**threshold 정합 — `options.thresholds`**:
- `matrix_duration_ms p(95) < 800` (REQ-NF-PER-001)
- `ranking_duration_ms p(95) < 1200` (REQ-NF-PER-002)
- `errors rate < 0.01` (REQ-NF-PER-007)

threshold 미충족 시 k6 exit code != 0 → CI gate fail.

---

## 3. UI 측정 (Lighthouse + AG Grid 자체 metric)

### Lighthouse CI 절차
```bash
cd frontend
npm run build
npx serve dist -p 4173
npx lighthouse http://localhost:4173/extrusion-matrix --preset=desktop --output=json
```

### AG Grid 측정 — 콘솔 hook
```ts
// 페이지 onGridReady 시 measure
const t0 = performance.now()
event.api.sizeColumnsToFit()
console.log('AG Grid first render:', performance.now() - t0)
```

### 목표
- **REQ-NF-PER-005** entry bundle ≤ 200kB gzip — Vite 빌드 통계 `dist/assets/index-*.js`
- **REQ-NF-PER-006** AG Grid first render ≤ 500ms — 1500 row × 30 col seed

---

## 4. Sprint 6 EP-40 진척 게이트

- [ ] k6 STG 5분 부하 — 모든 threshold pass
- [ ] Lighthouse entry bundle ≤ 200kB (현재 ant-design lazy 적용 필요 — EP-46 병합)
- [ ] AG Grid 1500 row first render ≤ 500ms (Sprint 5 가상 스크롤 확인)
- [ ] Grafana 대시 — matrix/ranking p95 시계열 (EP-44 통합)

---

## 5. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — k6 1500-row 매트릭스 부하 시나리오 + UI Lighthouse + 7 NFR threshold |
