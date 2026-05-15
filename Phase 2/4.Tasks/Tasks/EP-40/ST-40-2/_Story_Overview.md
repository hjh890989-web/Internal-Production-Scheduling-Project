# Story Overview — [EP-40] ST-40-2 성형·압출 후보 생성 SLO

**Sprint**: S2+S3 | **Epic**: EP-40 성능 NFR | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> SRS REQ-NF-PER-002·003: "1주 호라이즌 성형 후보 생성 p95 ≤ 5분, 압출 후보 p95 ≤ 2분"

EP-05 (GreedyRotationAllocator) + EP-09 (SettingGroupAllocator) 의 부하 검증. 1주 분량 (≈ 1,500 VC row + 600 EX row) 처리 p95.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-40-2-1](TK-40-2-1.md) | 1주 후보 생성 p95 측정 (k6 + 직접 API) | 0.8 | QA |
| [TK-40-2-2](TK-40-2-2.md) | 성형 ≤ 5분·압출 ≤ 2분 검증 | 0.7 | QA + Backend |
| [TK-40-2-3](TK-40-2-3.md) | 회귀 매트릭스 (다양 부하 규모) | 0.6 | QA + DevOps |

> **선행**: [EP-05](../../EP-05/), [EP-09](../../EP-09/), [TK-40-1-3](../ST-40-1/TK-40-1-3.md)
> **후행**: ST-40-3

## Story 레벨 DoD

- [ ] **k6 시나리오** vc-allocator-load.js + ex-allocator-load.js
- [ ] **성형 1주 p95 ≤ 300,000ms**
- [ ] **압출 1주 p95 ≤ 120,000ms**
- [ ] **회귀 매트릭스**: 0.5주 / 1주 / 2주 / 4주 부하
- [ ] **CI nightly + Grafana**

---

## References
- WBS §8.5 EP-40 ST-40-2
- SRS REQ-NF-PER: 002·003
- TestPlan: T-L

## 개정 이력
| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
