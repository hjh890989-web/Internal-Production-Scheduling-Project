# Story Overview — [EP-07] ST-07-1 압출 완료 기한 = 성형 투입 - 1일

**Sprint**: S3 | **Epic**: EP-07 D-1 자동 역산 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §6 EP-07 ST-07-1: "TK-07-1-1 `vc.confirmed` 이벤트 구독, TK-07-1-2 D-1 역산 로직, TK-07-1-3 모든 row `완료일 ≤ vc_date-1` 검증"
> SRS REQ-FUNC-EX-001 / BR-E01: "압출 완료 기한 = 성형 투입일 − 1 working day"

본 Story는 성형 스케줄 확정 시 발생하는 `vc.confirmed` 도메인 이벤트를 구독해 압출 D-1 deadline을 자동 산출한다. EP-06의 `WorkingCalendarService.subtractWorkingDays` 재사용. 후속 EP-08 (수식)·EP-09 (그룹핑)이 사용할 `ExScheduleCandidate.dueDate` 공급.

**Why Must (Sprint 3 핵심)**:
- 압출-성형 간 D-1 hard 제약 — 재공 정체 방지 (현장 실측 평균 2일 지연)
- EP-08 진입 전제 — 본 deadline 없이 압출 후보 생성 불가
- BR-E01 hard

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-07-1-1](TK-07-1-1.md) | `vc.confirmed` 이벤트 구독 + 핸들러 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-07-1-2](TK-07-1-2.md) | D-1 역산 로직 (`BackwardExtrusionCalculator`) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-07-1-3](TK-07-1-3.md) | 모든 row `완료일 ≤ vc_date-1` 검증 + 회귀 | 0.7 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-06 ST-06-1](../../EP-06/ST-06-1/_Story_Overview.md) (WorkingCalendarService), EP-05 ST-05-3 (`vc.confirmed` publisher)
> **후행**: EP-08 ST-08-1·2·3, EP-09 ST-09-1

---

## Story 레벨 DoD

- [ ] **`VcConfirmedEvent` 도메인 이벤트** 발행 측 확정 (EP-05) + 구독 측 핸들러
- [ ] **`BackwardExtrusionCalculator.compute(vcSchedule)`** — `Map<HoseId, LocalDate>` 반환
- [ ] **deadline 공식**: `ex_deadline = vc_production_date − 1 working day`
- [ ] **회귀 100건**: 모든 ExSchedule row `production_date ≤ deadline` 만족
- [ ] **BR-E01 위반 시 conflict** — `AllocationConflict.exDeadlineExceeded` 생성
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-07 ST-07-1
- **PDD-03**: M-07 §4 A1 T1.1
- **SRS REQ-FUNC**: REQ-FUNC-EX-001
- **BR**: BR-E01
- **TestPlan**: TC-EX-001
- **선행**: EP-06 ST-06-1, EP-05 ST-05-3
- **후행**: EP-08, EP-09

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-07 ST-07-1 |
