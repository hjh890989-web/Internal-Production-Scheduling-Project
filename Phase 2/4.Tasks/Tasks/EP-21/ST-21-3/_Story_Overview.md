# Story Overview — [EP-21] ST-21-3 `28422-08HA0` LP-01 단일 셋팅

**Sprint**: S2 | **Epic**: EP-21 v1.4 신규 Must | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.2 EP-21 ST-21-3: "TK-21-3-1 RuleEngine `machine_pin` 강제 함수, TK-21-3-2 동시 다중 슬롯 차단 (Σ ≤1), TK-21-3-3 회귀 (LP-02~04 배정 0건)"
> SRS REQ-FUNC-VC-024 (BR-V14): "`28422-08HA0` 품번은 LP-01에만 배치 가능하며, LP-01 내 동시 다중 슬롯 사용 불가 (Σ ≤ 1)."

본 Story는 **28422-08HA0** 단일 품번의 운영 룰을 RuleEngine으로 강제. VC_HOSE_RULE 테이블의 `machine_pin='LP-01'`, `max_concurrent_slots=1` 조합으로 표현된다. 회전 배치 시 후보 슬롯이 다른 LP·IC 호기일 경우 자동 거부.

**Why Must**:
- R-V08 (LP-01 단일 셋팅 호기 고장 시 백업) 대응 기반 — 운영 이벤트(라인 가용성)로 일시 우회 가능 (Phase 2+)
- BR-V14 hard 제약
- ST-21-2 (VC_HOSE_RULE 마스터) 직접 사용 사례

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-21-3-1](TK-21-3-1.md) | RuleEngine `machine_pin` 강제 함수 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-21-3-2](TK-21-3-2.md) | 동시 다중 슬롯 차단 (Σ ≤ max_concurrent_slots) | 0.4 | Backend | T-U + T-I | ☐ |
| [TK-21-3-3](TK-21-3-3.md) | 회귀 (LP-02~04 배정 0건) | 0.5 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-21-2](../ST-21-2/_Story_Overview.md)
> **후행**: 없음 (Sprint 2 마감 후 R-V08 운영 이벤트는 별도 Phase)

---

## Story 레벨 DoD

- [ ] `MachinePinRule.validate(hoseId, machineId)` 함수 — `machine_pin` 일치 검증
- [ ] `ConcurrentSlotsRule.validate(hoseId, date, machineId, plannedSlots)` — 동시 다중 슬롯 상한 검증
- [ ] 회귀: `28422-08HA0` 1주 시나리오에서 LP-02·LP-03·LP-04·IC 배정 0건
- [ ] LP-01 동시 다중 슬롯 사용 0건 (Σ ≤ 1)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21 ST-21-3
- **PDD-02 v1.1**: BR-V14
- **SRS REQ-FUNC**: REQ-FUNC-VC-024
- **TestPlan**: TC-VC-024
- **선행**: ST-21-2 (VC_HOSE_RULE 테이블·캐시)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 ST-21-3 |
