# Story Overview — [EP-08] ST-08-2 yield 수식 + BR-E05 reference 검증

**Sprint**: S3 | **Epic**: EP-08 압출 수식 | **Priority**: Must ⭐ (Sprint 3 가장 중요)
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §6 EP-08 ST-08-2: "TK-08-2-1 `floor(speed × min × 1000 / length)` 구현, TK-08-2-2 `29673-2R060` 주간전반 = 2,531 회귀 PASS, TK-08-2-3 단위 변환 (mm vs m) 가드"
> SRS REQ-FUNC-EX-005 / BR-E05 / 골든 케이스: "yield = floor(speed_m_per_min × effective_min × 1000 / length_mm)"

본 Story는 **압출 yield 수식**의 정식 구현 + **`29673-2R060` reference case 검증**. BR-E05 골든 회귀는 Sprint 3 DoD의 단일 가장 중요한 통과 게이트. 수식 오류 = 압출 후보 전체 신뢰도 붕괴.

**검증 사례** (BR-E05 reference):
- 품번: `29673-2R060`
- 압출 speed: 14.06 m/min
- length: 1000 mm
- shift: 주간전반 (effective_min = 180)
- 계산: `floor(14.06 × 180 × 1000 / 1000) = floor(2530.8) = 2531`

**Why Must**:
- BR-E05 reference case는 시스템 신뢰성의 1차 검증 (INT-1 사건 직접 차단)
- EP-09 (셋팅 그룹핑)·EP-EX11 (검증 게이트) 모두 yield 정확성 의존
- 단위 혼동 (mm vs m) 가드는 마스터 입력 오류로부터 시스템 보호

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-08-2-1](TK-08-2-1.md) | yield 수식 구현 (`YieldFormula`) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-08-2-2](TK-08-2-2.md) | `29673-2R060` BR-E05 reference 회귀 PASS | 0.6 | QA + Backend | T-I + A | ☐ |
| [TK-08-2-3](TK-08-2-3.md) | 단위 변환 가드 (mm vs m 명시적 에러) | 0.8 | Backend | T-U + T-I | ☐ |

> **선행**: [ST-08-1](../ST-08-1/_Story_Overview.md) (shift)
> **후행**: ST-08-3, EP-09, EP-EX11

---

## Story 레벨 DoD

- [ ] **`YieldFormula.compute(speed, effectiveMin, lengthMm)`** — `floor(speed × min × 1000 / length)`
- [ ] **`29673-2R060` 주간전반 yield = 2,531** PASS (TC-EX-005)
- [ ] **단위 가드** — `UnitMismatchException` (speed in mm/min 입력 시) 차단
- [ ] **0·음수·overflow 방어** — 명시적 에러
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-08 ST-08-2
- **PDD-03**: M-08 §4 A2
- **SRS REQ-FUNC**: REQ-FUNC-EX-005
- **BR**: BR-E05 (yield 수식)
- **TestPlan**: TC-EX-005

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-08 ST-08-2 |
