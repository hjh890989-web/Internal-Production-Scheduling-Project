# Story Overview — [EP-40] ST-40-1 수주 Import 지연 ≤60초 (10K row)

**Sprint**: S0+S2 | **Epic**: EP-40 성능 NFR | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §8.5 EP-40 ST-40-1 / SRS REQ-NF-PER-001: "10,000 row 수주 import → 마스터 commit p95 ≤ 60초"

EP-01 (수주 통합) 의 부하 검증. 10K row Excel 파일 → 검증 → DB INSERT 전체 p95 60초. Sprint 1 단위 측정으로는 정확성 어려움 — k6 부하 도구로 명시적 SLO 회귀.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-40-1-1](TK-40-1-1.md) | 부하 시나리오 작성 (10K row fixture) | 0.7 | QA |
| [TK-40-1-2](TK-40-1-2.md) | k6/Gatling 스크립트 + JMeter (alternative) | 0.7 | QA + Backend |
| [TK-40-1-3](TK-40-1-3.md) | p95 ≤ 60초 회귀 게이트 (CI nightly) | 0.7 | QA + DevOps |

> **선행**: [EP-01](../../EP-01/), [EP-31](../../EP-31/) (관측성), [EP-32](../../EP-32/) (CI)
> **후행**: ST-40-2

---

## Story 레벨 DoD

- [ ] **10K row Excel fixture** — 47품번 × 평균 200건 분포
- [ ] **k6 script `import-load.js`** — POST `/api/v1/import` × 부하 변형
- [ ] **CI nightly 자동 실행** — Jenkins pipeline 통합
- [ ] **p95 ≤ 60,000ms** 회귀 PASS — Grafana 패널 + Slack 알림 (위반 시)
- [ ] **SRS REQ-NF-PER-001** 정합

---

## References

- **WBS**: §8.5 EP-40 ST-40-1
- **SRS REQ-NF-PER**: 001
- **TestPlan**: T-L import

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
