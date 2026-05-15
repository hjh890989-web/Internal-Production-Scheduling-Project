# Story Overview — [EP-VC15] ST-VC15-1 G_VAL 실패 시 ≥3 대안 충돌 리포트

**Sprint**: S2 | **Epic**: EP-VC15 충돌 리포트 ≥3 대안 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-VC15 ST-VC15-1: "TK-VC15-1-1 충돌 분류기, TK-VC15-1-2 대안 생성, TK-VC15-1-3 모든 충돌 리포트가 ≥3 distinct 대안 포함 회귀"
> SRS REQ-FUNC-VC-015: "검증 게이트 실패 시 분류 + ≥3 distinct 대안."

본 Story는 EP-04·05·06·21의 `AllocationConflict` 객체를 **카테고리화 + 대안 enrich** 하여 사용자에게 통합 리포트로 제공. UI 데이터 소스.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-VC15-1-1](TK-VC15-1-1.md) | 충돌 분류기 (Conflict Categorizer) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-VC15-1-2](TK-VC15-1-2.md) | 대안 생성기 (Alternative Generator) | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-VC15-1-3](TK-VC15-1-3.md) | ≥3 distinct 대안 회귀 + API | 0.6 | QA + Backend | T-I + A | ☐ |

> **선행**: EP-04, EP-05, EP-06, EP-21
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] **ConflictCategory enum**: SLOT_OX, ANGLE_CAPA, DAILY_CAPA, DEADLINE_D2, DAY_LOCK, LEFT_RIGHT, MACHINE_PIN, SPEC_LT7
- [ ] **AlternativeType enum**: NIGHT_ROTATION, DEADLINE_NEGOTIATE, IC_ROUTING, OUTSOURCE
- [ ] **AlternativeGenerator** — 카테고리별 ≥ 3 distinct 대안 추천
- [ ] **회귀 100건** — 모든 충돌 리포트에 ≥ 3 distinct 대안
- [ ] **API**: `/api/v1/schedule/conflicts/{scheduleId}` p95 ≤ 1초
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-VC15 ST-VC15-1
- **SRS REQ-FUNC**: REQ-FUNC-VC-015
- **TestPlan**: TC-VC-015

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
