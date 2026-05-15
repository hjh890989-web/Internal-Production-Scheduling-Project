# Story Overview — [EP-EX12] ST-EX12-1 G_VAL 실패 시 ≥3 대안

**Sprint**: S3 | **Epic**: EP-EX12 압출 충돌 대안 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §6 EP-EX12 ST-EX12-1: "TK-EX12-1-1 대안 생성 알고리즘, TK-EX12-1-2 모든 실패 케이스 ≥3 대안 회귀"
> SRS REQ-FUNC-EX-012: "≥ 3 distinct 대안 (조기 시작·야간 후반·성형 투입일 협상·외주)"

본 Story는 EP-EX11이 만든 `ValidationResult.fail` 결과를 입력으로 받아 **압출 도메인 4 base 대안**을 enrich. EP-VC15 (성형) 패턴 그대로 — `AlternativeType + Generator + Policy 매트릭스`. 차이점: 도메인별 대안 종류 + 적용 가능 여부 정책.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-EX12-1-1](TK-EX12-1-1.md) | 대안 생성 알고리즘 (`ExtrusionAlternativeGenerator`) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-EX12-1-2](TK-EX12-1-2.md) | 모든 실패 케이스 ≥ 3 대안 회귀 + API | 0.7 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-EX11](../../EP-EX11/), [EP-VC15](../../EP-VC15/)
> **후행**: EP-EX13 (Sprint 3~4)

---

## Story 레벨 DoD

- [ ] **`ExtrusionAlternativeType` enum**: EARLIER_START, NIGHT_SECOND_BOOST, VC_DATE_NEGOTIATE, OUTSOURCE, OVERTIME, SWAP_CANDIDATE
- [ ] **POLICY 매트릭스**: CUMULATIVE_YIELD_SHORT × 대안 / SHIFT_CAPACITY_EXCEEDED × 대안 적용 가능 여부
- [ ] **회귀 100건** — 모든 fail 케이스 ≥ 3 distinct
- [ ] **API**: `/api/v1/schedule/extrusion/conflicts/{candidateId}` p95 ≤ 1초
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-EX12 ST-EX12-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-012
- **TestPlan**: TC-EX-012

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-EX12 ST-EX12-1 |
