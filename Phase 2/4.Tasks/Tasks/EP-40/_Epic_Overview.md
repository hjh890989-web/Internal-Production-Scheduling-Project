# Epic Overview — [EP-40] 성능 NFR (Performance)

**Sprint**: S0(기반) + S2·S3(엔진)·S4(WebSocket)·S5(부하 테스트) | **Priority**: Must ⭐ | **SP 합계**: 13 | **PD 추정**: ~9.1 PD

---

## Epic 목적

> WBS §8.5 EP-40 인용: "SRS §4.2.1 비기능 요구사항 (PER 8건) 분해 — 부하·SLO·RUM·on-demand 검사"
> SRS REQ-NF-PER-001~008: 수주 import 60초, 후보 생성 5분/2분, PUSH 60초/2초, UI 1초, on-demand 3초.

본 Epic은 **모든 기능 Epic의 성능 검증 횡단**. 측정 도구 (k6·Gatling·JMeter·RUM·Prometheus) + 회귀 게이트 (CI nightly) + Grafana 패널 통합. Sprint 0 도구 셋업 → Sprint 2~5 분산 측정.

**Why P0 (Phase 3 진입 게이트)**:
- 성능 SLO 미정의 = Phase 3 측정 불가
- 회귀 기준 부재 = 성능 저하 catch 불가
- REQ-NF-PER-001~008 직접 매핑 (NFR 60건 중 8건)

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | NFR | 상태 |
|---|---|:--:|:--:|:--:|:--:|:--:|
| [ST-40-1](ST-40-1/_Story_Overview.md) | 수주 Import 지연 ≤60초 (10K row) | 3 | ~2.1 | T-L | PER-001 | ☐ |
| [ST-40-2](ST-40-2/_Story_Overview.md) | 성형·압출 후보 생성 SLO | 3 | ~2.1 | T-L | PER-002·003 | ☐ |
| [ST-40-3](ST-40-3/_Story_Overview.md) | WebSocket PUSH·Critical 알림 SLO | 3 | ~2.1 | T-S | PER-004 | ☐ |
| [ST-40-4](ST-40-4/_Story_Overview.md) | UI·드래그앤드롭·인지 RT SLO | 2 | ~1.4 | RUM | PER-005·006·008 | ☐ |
| [ST-40-5](ST-40-5/_Story_Overview.md) | 전체 스케줄 on-demand 검사 ≤3초 | 2 | ~1.4 | T-L | PER-007 | ☐ |

---

## Epic 레벨 DoD

- [ ] **`k6` 부하 시나리오** — 5 시나리오 (import·VC·EX·WebSocket·on-demand)
- [ ] **Grafana 패널** — p50·p95·p99 트렌드 5종
- [ ] **CI nightly 회귀 게이트** — 모든 p95 threshold 위반 시 빌드 실패
- [ ] **RUM (Real User Monitoring)** — Sentry Performance 또는 Datadog RUM
- [ ] **NFR 8건 100% 측정** — REQ-NF-PER-001~008
- [ ] 부하 테스트 ≥ 80% 기능 커버리지

---

## References

- **WBS**: §8.5 EP-40
- **SRS REQ-NF-PER**: 001~008
- **TestPlan**: T-L (Load Test) 카테고리
- **선행**: EP-00 (인프라), EP-31 (관측성), EP-32 (CI/CD)
- **후행**: 없음 (지속 회귀)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §8.5 EP-40 + REQ-NF-PER-001~008 |
