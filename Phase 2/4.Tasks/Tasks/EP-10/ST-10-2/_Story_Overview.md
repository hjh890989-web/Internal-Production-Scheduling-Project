# Story Overview — [EP-10] ST-10-2 확정 게이트 (EX)

**Sprint**: S4 | **Epic**: EP-10 사용자 확정 게이트 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §7 EP-10 ST-10-2: "TK-10-2-1 EX 동일 패턴 적용, TK-10-2-2 통합 테스트"
> SRS REQ-FUNC-EX-019 / BR-X01: "EX 스케줄은 Planner role 명시적 확정으로만 Candidate → Confirmed 전이"

본 Story는 ST-10-1 VC 패턴을 EX (압출) 도메인에 적용. `ExScheduleStatus` enum + `ExSchedule.status/confirmedAt/confirmedBy` + Flyway V4_2 + planner_role function + confirm API. 코드/DDL 패턴 동일, 도메인 분리.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-10-2-1](TK-10-2-1.md) | EX 상태 머신 + Confirm API (VC 패턴 적용) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-10-2-2](TK-10-2-2.md) | EX 확정 게이트 통합 테스트 + negative 회귀 | 0.7 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-10-1](../ST-10-1/_Story_Overview.md), [EP-09](../../EP-09/)
> **후행**: EP-11, EP-EX13

---

## Story 레벨 DoD

- [ ] **`ExScheduleStatus` enum**: DRAFT, CANDIDATE, CONFIRMED
- [ ] **V4_2 ALTER TABLE** ex_schedule (status·confirmed_at·confirmed_by + 트리거)
- [ ] **`ex_schedule_confirm(uuid, varchar)` function** + planner_role GRANT
- [ ] **API `POST /api/v1/schedule/ex/{id}/confirm`** Planner role
- [ ] **`ExConfirmedEvent` 발행** — EP-30 (PUSH) 입력
- [ ] **negative 회귀**: app_user 직접 UPDATE 차단
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §7 EP-10 ST-10-2
- **SRS REQ-FUNC**: REQ-FUNC-EX-019
- **BR**: BR-X01
- **TestPlan**: TC-EX-019

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-10 ST-10-2 |
