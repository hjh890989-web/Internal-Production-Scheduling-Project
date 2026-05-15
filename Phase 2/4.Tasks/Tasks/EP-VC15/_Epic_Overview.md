# Epic Overview — [EP-VC15] 충돌 리포트 ≥3 대안

**Sprint**: S2 (성형 핵심) | **Priority**: Must ⭐ | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §5.2 EP-VC15 인용: "검증 게이트 실패 시 시스템은 실패 유형을 분류하고 최소 3종 대안을 제시하는 충돌 리포트를 반환해야 한다."
> SRS REQ-FUNC-VC-015: "G_VAL 실패 시 카테고리(slot O/X·angle capa·daily capa·D-2·당일 락 위반 등) + ≥3 distinct 대안 (야간 회전 추가·납기 협상·IC 라우팅 전환·외주)."

본 Epic은 **EP-04·EP-05·EP-06·EP-21** 의 모든 검증 실패를 통합 분류 + 대안 제시. v1.2 명시화(REV-D-003) 결정 — Sprint 2 핵심 deliverable. 사용자(생산계획팀)가 충돌을 빠르게 이해하고 의사결정할 수 있는 UI 데이터 소스.

**Why Must (Sprint 2)**:
- EP-04~21의 모든 conflict 통합 인터페이스 — Sprint 2 DoD의 시연 핵심
- "충돌 발견 후 어떻게 해소할까?" 의 시스템 가이드 — INT-1 사건 재발 차단
- ST-VC16과 함께 검증 게이트 완성도 결정

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-VC15-1](ST-VC15-1/_Story_Overview.md) | G_VAL 실패 시 ≥3 대안 충돌 리포트 | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **충돌 분류기** — slot O/X·angle capa·daily capa·D-2·당일 락·좌/우·machine_pin·spec<7 등 카테고리화
- [ ] **대안 생성기** — 4종 (야간 회전 추가·납기 협상·IC 라우팅 전환·외주) ≥ 3 distinct
- [ ] **회귀 100건** — 모든 충돌 리포트가 ≥ 3 distinct 대안 포함
- [ ] **API** `/api/v1/schedule/conflicts/{scheduleId}` — JSON 응답 ≤ 1초
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-VC15
- **SRS REQ-FUNC**: REQ-FUNC-VC-015
- **PDD-02 v1.2**: §4 G_VAL
- **TestPlan**: TC-VC-015
- **선행**: EP-04, EP-05, EP-06, EP-21 (AllocationConflict 생성 측)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-VC15 + REQ-FUNC-VC-015 |
