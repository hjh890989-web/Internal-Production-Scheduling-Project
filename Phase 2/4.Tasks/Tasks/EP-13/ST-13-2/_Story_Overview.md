# Story Overview — [EP-13] ST-13-2 RuleEngine 일중 교체 차단

**Sprint**: S4 | **Epic**: EP-13 당일 락 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-13 ST-13-2: "TK-13-2-1 RuleEngine `intra_day_lock_ok` 함수, TK-13-2-2 1주 호라이즌 회귀 (일중 교체 0건), TK-13-2-3 후보 생성 시 차단"
> SRS REQ-FUNC-VC-012 / BR-V07 / ADR-016: "RuleEngine은 후보 생성 시 동일 (machine, slot, date) 슬롯에 다른 hose 배치를 차단해야 함."

본 Story는 ADR-016 4-layer 강제 중 2번째 (RuleEngine). DB 제약은 commit 시점 검증 — Allocator 후보 생성 단계에서 미리 차단하면 conflict 메시지·재계산 비용 절감. `IntraDayLockRule`을 RulePipeline에 통합.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-13-2-1](TK-13-2-1.md) | RuleEngine `IntraDayLockRule` 함수 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-13-2-2](TK-13-2-2.md) | 1주 호라이즌 회귀 (일중 교체 0건) | 0.7 | QA + Backend | T-I + A | ☐ |
| [TK-13-2-3](TK-13-2-3.md) | 후보 생성 시 차단 (Allocator 통합) | 0.7 | Backend | T-U + T-I | ☐ |

> **선행**: [ST-13-1](../ST-13-1/_Story_Overview.md), [EP-21](../../EP-21/) (RulePipeline)
> **후행**: ST-13-3, ST-13-4

---

## Story 레벨 DoD

- [ ] **`IntraDayLockRule.evaluate(candidate, ledger)`** — (machine, slot, date) ledger 조회 후 다른 hose 차단
- [ ] **RulePipeline 통합** — 7번째 Rule (SlotOx·MachinePin·LeftRight·HoseSlotCap·SpecLt7·IntraDayLock·...)
- [ ] **회귀 100건 시나리오** — 일중 교체 시도 0건 + conflict 분류 정확
- [ ] **EP-VC15 conflict category** — INTRA_DAY_LOCK 추가
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-13 ST-13-2
- **SRS REQ-FUNC**: REQ-FUNC-VC-012
- **BR**: BR-V07
- **TestPlan**: TC-VC-012

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
