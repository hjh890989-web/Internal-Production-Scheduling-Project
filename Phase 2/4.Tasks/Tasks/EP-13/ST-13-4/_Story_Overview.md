# Story Overview — [EP-13] ST-13-4 사용자 override 모달 + 사유 강제

**Sprint**: S4 | **Epic**: EP-13 당일 락 | **Priority**: Must
**SP 합계**: 1 | **PD 추정**: ~0.7 PD

---

## Story 목적

> WBS §7 EP-13 ST-13-4: "TK-13-4-1 일중 교체 시도 시 모달 표시 UI, TK-13-4-2 사유 텍스트 강제 (REQ-FUNC-CO-010), TK-13-4-3 audit 사유 기록 통합"
> SRS REQ-FUNC-VC-014·CO-010: "당일 락 일중 교체 시도 시 모달로 사유 입력 강제. 사유 없이 진행 불가. audit에 사유 기록."

본 Story는 ADR-016 4-layer 중 4번째 (사용자 override). 정상적으로 BR-V07 차단되는 후보를 운영팀이 긴급 사유로 강제 진행 가능 — 단, 사유 텍스트 30자 이상 + Planner role + audit 기록. UI는 Sprint 5에서 정식 구현, 본 Story는 API + 검증 logic.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-13-4-1](TK-13-4-1.md) | 일중 교체 override API + 모달 응답 형식 | 0.3 | Backend | T-U + T-I | ☐ |
| [TK-13-4-2](TK-13-4-2.md) | 사유 텍스트 강제 (≥ 30자) | 0.2 | Backend | T-U + T-I | ☐ |
| [TK-13-4-3](TK-13-4-3.md) | audit 사유 기록 통합 테스트 | 0.2 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-13-1](../ST-13-1/_Story_Overview.md), [ST-13-2](../ST-13-2/_Story_Overview.md), [EP-11](../../EP-11/) (audit)
> **후행**: Sprint 5 UI 모달

---

## Story 레벨 DoD

- [ ] **`POST /api/v1/schedule/vc/{id}/override-intraday-lock`** Planner role
- [ ] **request body**: `{ reason: string, newHoseId: string }` — reason ≥ 30자
- [ ] **`VcLockOverrideService`** — 사유 검증 + audit + 강제 INSERT (DEFERRED constraint 동작)
- [ ] **@Auditable reason** 자동 포함 (EP-11 결합)
- [ ] **negative**: reason < 30자 → 400 Bad Request
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-13 ST-13-4
- **SRS REQ-FUNC**: REQ-FUNC-VC-014, REQ-FUNC-CO-010
- **BR**: BR-V07
- **TestPlan**: TC-VC-014

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
