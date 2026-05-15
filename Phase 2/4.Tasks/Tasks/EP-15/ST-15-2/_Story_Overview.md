# Story Overview — [EP-15] ST-15-2 현장 피드백 1클릭 수용 채널

**Sprint**: S5 | **Epic**: EP-15 성형 현장 시뮬뷰 | **Priority**: Should ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §8 EP-15 ST-15-2: "TK-15-2-1 순서 조정 제안 UI, TK-15-2-2 1클릭 수용 (총량 보존), TK-15-2-3 통합 테스트"
> SRS REQ-FUNC-VC-018: "현장 작업자가 시뮬뷰에서 순서 조정 제안 가능. Planner가 1클릭 수용 시 총량 보존."

본 Story는 EP-15 ST-15-1의 양방향 UX 완결. 현장 작업자 (STK_USER) 가 cell 우클릭 → "순서 변경 제안" → Planner (PLANNER role) 에게 알림 → 1클릭 "수용" 또는 "거절". 수용 시 시간 cell만 swap (총 yield 보존).

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-15-2-1](TK-15-2-1.md) | 순서 조정 제안 UI (drag-and-drop 또는 우클릭 메뉴) | 0.5 | Frontend | T-U + T-I | ☐ |
| [TK-15-2-2](TK-15-2-2.md) | 1클릭 수용 API + 총량 보존 검증 | 0.5 | Backend | T-U + T-I + A | ☐ |
| [TK-15-2-3](TK-15-2-3.md) | 통합 테스트 (E2E 제안 → 수용 → 갱신) | 0.4 | QA + Frontend | T-I + A | ☐ |

> **선행**: [ST-15-1](../ST-15-1/_Story_Overview.md), [EP-10](../../EP-10/) (Planner role)
> **후행**: 없음 (Epic 마지막)

---

## Story 레벨 DoD

- [ ] **`vc_schedule_swap_proposal`** 테이블 + status (PROPOSED/ACCEPTED/REJECTED)
- [ ] **제안 UI**: 시뮬뷰 cell 우클릭 → "다른 회전으로 변경 제안" 모달
- [ ] **알림**: Planner에게 WebSocket PUSH (`/topic/vc-proposals`)
- [ ] **수용 API**: `POST /api/v1/schedule/vc/proposals/{id}/accept` Planner role
- [ ] **총량 보존**: swap 후 hose별 일별 총 yield 변경 0건 (BR 정합성)
- [ ] **거절 API**: `POST .../reject` + 사유
- [ ] 단위 + 통합 + audit 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-15 ST-15-2
- **SRS REQ-FUNC**: REQ-FUNC-VC-018
- **TestPlan**: TC-VC-018

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
