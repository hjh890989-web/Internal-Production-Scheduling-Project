# Story Overview — [EP-21] ST-21-4 `28422-2M800` 우측·≤2 + `28421-2M800` 좌측·≤2

**Sprint**: S2 | **Epic**: EP-21 v1.4 신규 Must | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.2 EP-21 ST-21-4: "TK-21-4-1 RuleEngine 품번 단위 상한 함수, TK-21-4-2 좌/우 + ≤2 결합 검증, TK-21-4-3 회귀 통과"
> SRS REQ-FUNC-VC-025·026 (BR-V15·V16): "`28422-2M800` 품번은 우측 슬롯에만 배치 가능하며 회전당 ≤ 2 슬롯. `28421-2M800` 품번은 좌측 슬롯에만 배치 가능하며 회전당 ≤ 2 슬롯."

본 Story는 **ST-21-1 (좌/우)** 와 **ST-21-2/ST-21-3 (max_concurrent_slots)** 를 **결합**해 두 가지 품번에 대한 복합 룰을 검증. 단일 룰 통과해도 결합 시 위반될 수 있는 경계 케이스 (좌측 3슬롯 시도)를 회귀로 차단.

**Why Must**:
- 좌/우 + 회전 상한 결합은 가장 까다로운 제약 조합 (개별 통과 ≠ 결합 통과)
- BR-V15·V16 hard 제약
- ST-21-1·ST-21-3 통합 검증 시나리오

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-21-4-1](TK-21-4-1.md) | RuleEngine 품번 단위 상한 함수 (HoseSlotCapRule) | 0.4 | Backend | T-U + T-I | ☐ |
| [TK-21-4-2](TK-21-4-2.md) | 좌/우 + ≤2 결합 검증 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-21-4-3](TK-21-4-3.md) | 회귀 통과 (`28422-2M800`·`28421-2M800`) | 0.5 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-21-1](../ST-21-1/_Story_Overview.md), [ST-21-2](../ST-21-2/_Story_Overview.md), [ST-21-3](../ST-21-3/_Story_Overview.md)
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] `HoseSlotCapRule` — VC_HOSE_RULE.max_concurrent_slots + side_lock 결합 검증
- [ ] **결합 시나리오**: 좌측 3슬롯 시도 → 1슬롯 거부 (≤2 위반)
- [ ] **결합 시나리오**: 우측 슬롯 사용 시 28421-2M800 거부 (side_lock=LEFT 위반)
- [ ] 회귀 100건 — `28421-2M800` LEFT only/≤2, `28422-2M800` RIGHT only/≤2 모두 만족
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21 ST-21-4
- **PDD-02 v1.1**: BR-V15·V16
- **SRS REQ-FUNC**: REQ-FUNC-VC-025·026
- **TestPlan**: TC-VC-025·026
- **선행**: ST-21-1, ST-21-2, ST-21-3

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 ST-21-4 |
