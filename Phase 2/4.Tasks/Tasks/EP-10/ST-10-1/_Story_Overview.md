# Story Overview — [EP-10] ST-10-1 Candidate → Confirmed 전이 게이트 (VC)

**Sprint**: S4 | **Epic**: EP-10 사용자 확정 게이트 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-10 ST-10-1: "TK-10-1-1 상태 머신 (Draft/Candidate/Confirmed), TK-10-1-2 Planner role RBAC + 트리거, TK-10-1-3 직접 DB 쓰기 차단 negative 테스트"
> SRS REQ-FUNC-VC-019 / CON-07: "VC 스케줄은 Planner role의 명시적 확정 행위로만 Candidate → Confirmed 전이 가능."

본 Story는 **VC 도메인 상태 머신**의 정식 구현. EP-05에서 만든 VcSchedule이 자동 Confirmed 되지 않고, Planner가 UI에서 명시적 "확정" 클릭 시 trigger. DB 트리거로 직접 UPDATE 시도 차단.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-10-1-1](TK-10-1-1.md) | 상태 머신 (Draft/Candidate/Confirmed) + Entity 확장 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-10-1-2](TK-10-1-2.md) | Planner role RBAC + 확정 트리거 + `vc.confirmed` 이벤트 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-10-1-3](TK-10-1-3.md) | 직접 DB 쓰기 차단 negative 테스트 + 회귀 | 0.7 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-05 ST-05-3](../../EP-05/ST-05-3/_Story_Overview.md)
> **후행**: ST-10-2, EP-11, EP-EX13

---

## Story 레벨 DoD

- [ ] **`VcScheduleStatus` enum**: DRAFT, CANDIDATE, CONFIRMED
- [ ] **VcSchedule entity** — `status` + `confirmedAt` + `confirmedBy` 컬럼
- [ ] **API**: `POST /api/v1/schedule/vc/{id}/confirm` — Planner role
- [ ] **DB trigger**: 잘못된 상태 전이 (Draft→Confirmed) 차단
- [ ] **RBAC**: app_user는 UPDATE status 직접 불가, planner_role만 함수 호출 가능
- [ ] **negative 테스트**: app_user → UPDATE 시도 → 403/SQL exception
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §7 EP-10 ST-10-1
- **PDD-04**: M-10 §4 A1
- **SRS REQ-FUNC**: REQ-FUNC-VC-019
- **BR**: BR-X01
- **TestPlan**: TC-VC-019

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-10 ST-10-1 |
