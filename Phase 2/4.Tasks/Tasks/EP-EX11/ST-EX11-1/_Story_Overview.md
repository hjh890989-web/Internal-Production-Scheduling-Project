# Story Overview — [EP-EX11] ST-EX11-1 압출 검증 게이트 (p95 ≤ 2초 pass/fail)

**Sprint**: S3 | **Epic**: EP-EX11 압출 검증 게이트 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §6 EP-EX11 ST-EX11-1: "TK-EX11-1-1 누적 yield ≥ Q_ext 검증, TK-EX11-1-2 shift capacity 초과 체크, TK-EX11-1-3 후보당 pass/fail p95 ≤2s"
> SRS REQ-FUNC-EX-011: "누적 shift yield ≥ Q_ext + shift 용량 ≤ effective_min, 후보당 p95 ≤ 2초"

본 Story는 EP-09의 SettingGroupAllocator 결과를 candidate별로 검증. **2가지 검증 조건** + **p95 ≤ 2초 성능 게이트**.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-EX11-1-1](TK-EX11-1-1.md) | 누적 yield ≥ Q_ext 검증 (deadline 이전 합산) | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-EX11-1-2](TK-EX11-1-2.md) | shift capacity 초과 체크 (`actualMin ≤ effective_min`) | 0.4 | Backend | T-U + T-I | ☐ |
| [TK-EX11-1-3](TK-EX11-1-3.md) | 후보당 pass/fail p95 ≤ 2초 + 측정 | 0.5 | QA + Backend | T-P + A | ☐ |

> **선행**: [EP-08](../../EP-08/), [EP-09](../../EP-09/)
> **후행**: EP-EX12

---

## Story 레벨 DoD

- [ ] **`ExtrusionValidationGate.validate(candidate, ledger)`** — record 결과 반환
- [ ] **검증 조건 1**: `Σ yield(deadline 이전) ≥ Q_ext` (BR-E10)
- [ ] **검증 조건 2**: `Σ actualMin(shift) ≤ effective_min` (BR-E04)
- [ ] **p95 ≤ 2,000ms** (600 후보)
- [ ] **결과 record**: `ValidationResult { passed, violations, measuredAt }`
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-EX11 ST-EX11-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-011
- **SRS REQ-NF-PERF**: REQ-NF-PERF-003
- **TestPlan**: TC-EX-011

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-EX11 ST-EX11-1 |
