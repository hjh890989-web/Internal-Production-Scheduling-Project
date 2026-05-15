# Story Overview — [EP-EX13] ST-EX13-1 `vc.changed` 이벤트 자동 재계산

**Sprint**: S3~S4 | **Epic**: EP-EX13 성형 변경 자동 트리거 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §6 EP-EX13 ST-EX13-1: "TK-EX13-1-1 `vc.changed` 구독자 등록, TK-EX13-1-2 영향 EX row 식별, TK-EX13-1-3 partial replan 자동 트리거, TK-EX13-1-4 100건 시뮬 100% 재계획 회귀"

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-EX13-1-1](TK-EX13-1-1.md) | `VcChangedEvent` publisher + subscriber | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-EX13-1-2](TK-EX13-1-2.md) | 영향 EX row 식별 알고리즘 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-EX13-1-3](TK-EX13-1-3.md) | Partial replan 자동 트리거 | 0.6 | Backend | T-U + T-I | ☐ |
| [TK-EX13-1-4](TK-EX13-1-4.md) | 100건 시뮬 100% 재계획 회귀 | 0.5 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-10](../../EP-10/), [EP-EX11](../../EP-EX11/)
> **후행**: EP-EX14

---

## Story 레벨 DoD

- [ ] **`VcChangedEvent`** record (scheduleId, changedRows[])
- [ ] **Publisher** — VC override (TK-13-4-1) + UPDATE 시 자동 발행
- [ ] **`VcChangedListener`** — partial replan service 호출
- [ ] **영향 row 식별**: hose_id + production_date 범위 (ex_deadline 영향)
- [ ] **회귀 100 시나리오** — 모든 변경 자동 처리 (수동 0건)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §6 EP-EX13 ST-EX13-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-013
- **BR**: BR-X03, BR-E11

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
