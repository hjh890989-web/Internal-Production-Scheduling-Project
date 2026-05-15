# Story Overview — [EP-05] ST-05-4 저압 ↔ IC 라우팅

**Sprint**: S2 | **Epic**: EP-05 회전수 배치 (M-05) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.2 EP-05 인용: "ST-05-4 — TK-05-4-1 라우팅 정책 (저압 우선), TK-05-4-2 라우팅 로그 audit, TK-05-4-3 회귀 (저압 포화 후 IC)"
> SRS REQ-FUNC-VC-011 / BR-V08 인용: "시스템은 두 유형 모두 가능한 hose_id를 저압 우선 배정하고, 저압 회전이 포화된 경우에만 IC로 폴백해야 한다."

본 Story는 ST-05-3의 단순 greedy 알고리즘 내부에 묶여 있는 **저압 우선 라우팅 정책을 외부화·정밀화**. ADR-006(저압 우선 라우팅) 정합. 정책 변경(예: IC 우선·균등 분배) 시 코드 무수정·룰만 갱신.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-05-4-1](TK-05-4-1.md) | 라우팅 정책 (저압 우선 + 외부화) | 0.5 | Backend | T-U + I | ☐ |
| [TK-05-4-2](TK-05-4-2.md) | 라우팅 로그 audit (machine_decision 테이블) | 0.4 | Backend | T-I + I | ☐ |
| [TK-05-4-3](TK-05-4-3.md) | 회귀 (저압 포화 후 IC 폴백) | 0.5 | QA + Backend | T-I + A | ☐ |

> **선행 의존**: [TK-05-3-2](../ST-05-3/TK-05-3-2.md) (GreedyRotationAllocator)
> **후행 차단**: 없음 (EP-05 마지막)

---

## Story 레벨 DoD

- [ ] **저압 우선 라우팅** 정확 (BR-V08, ADR-006)
- [ ] **저압 포화 시에만 IC 사용**
- [ ] **라우팅 정책 외부화** — application.yaml 또는 admin endpoint
- [ ] **라우팅 audit** — 매 결정 기록 (PRD §15 ADR-006 정신)
- [ ] **회귀**: 저압 자격 100 품번 → 100% 저압 우선, 포화 시 IC 폴백
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §5.2 EP-05 ST-05-4
- **SAD**: §10 ADR-006 (신규 우선 라우팅 — 본 결정의 성형판)
- **SRS REQ-FUNC**: REQ-FUNC-VC-011
- **BR**: BR-V08 (저압 우선)
- **TestPlan**: TC-VC-011 (저압 포화 후 IC)
- **연관**: 선행 [ST-05-3](../ST-05-3/_Story_Overview.md), 후속 없음 (EP-05 마지막 Story)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-05 ST-05-4 + BR-V08 + ADR-006 + REQ-FUNC-VC-011 |
