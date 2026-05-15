# Epic Overview — [EP-EX11] 압출 검증 게이트 (v1.2 명시화 — REV-D-003)

**Sprint**: S3 (압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Epic 목적

> WBS §6 EP-EX11 인용: "ST-EX11-1 압출 검증 게이트 (p95 ≤2초 pass/fail). TK-EX11-1-1 누적 yield ≥ Q_ext 검증, TK-EX11-1-2 shift capacity 초과 체크, TK-EX11-1-3 후보당 pass/fail p95 ≤2s"
> SRS REQ-FUNC-EX-011: "누적 shift yield가 기한 이전 Q_ext를 충족하고 shift 용량을 초과하지 않는지 후보당 p95 ≤2초 검증"

본 Epic은 EP-09 그룹핑 결과의 **수치 정합성** 자동 검증. 두 가지 핵심 조건:
1. **수량 충족**: deadline 이전 누적 yield ≥ Q_ext (BR-E10)
2. **용량 한계**: shift effective_min 초과 0건 (BR-E04)

EP-VC15 (성형 검증 게이트)의 압출 대응 — 검증 결과는 EP-EX12 (충돌 대안)의 입력. p95 ≤ 2초는 1주 호라이즌 ≈ 600 후보 처리 성능 게이트.

**Why Must (Sprint 3)**:
- **REV-D-003 명시화** — Sprint 3 DoD 검증 자동화
- **EP-EX12 진입 전 게이트** — 실패 시 ≥ 3 대안 제시
- **p95 2초** — UI 응답성 직결 (사용자 후보 검토 시 즉시 피드백)

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-EX11-1](ST-EX11-1/_Story_Overview.md) | 압출 검증 게이트 (p95 ≤ 2초 pass/fail) | 2 | ~1.4 | T-U + T-I + T-P | ☐ |

---

## Epic 레벨 DoD

- [ ] **`ExtrusionValidationGate.validate(candidate)`** — pass/fail + 위반 카테고리
- [ ] **누적 yield ≥ Q_ext** — deadline 이전 (shift 단위 누계)
- [ ] **shift capacity** — 누적 actualMin ≤ effective_min (180)
- [ ] **p95 ≤ 2,000ms** — 600 후보 측정
- [ ] **결과 record**: `{ candidateId, passed, violations: [...], measuredAt }`
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-EX11
- **SRS REQ-FUNC**: REQ-FUNC-EX-011
- **SRS REQ-NF-PERF**: REQ-NF-PERF-003 (검증 게이트 응답 시간)
- **PDD-03**: M-10 검증 게이트
- **TestPlan**: TC-EX-011
- **선행**: EP-08 (yield + Q_ext), EP-09 (그룹핑)
- **후행**: EP-EX12 (충돌 대안), EP-EX13 (성형 변경 트리거)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-EX11 + REQ-FUNC-EX-011 |
