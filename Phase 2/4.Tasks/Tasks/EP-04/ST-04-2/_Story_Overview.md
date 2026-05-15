# Story Overview — [EP-04] ST-04-2 스케줄 불가 품번 사전 제외

**Sprint**: S2 | **Epic**: EP-04 슬롯 O/X 검증 (M-04) | **Priority**: Must
**SP 합계**: 1 | **PD 추정**: ~0.7 PD

---

## Story 목적

> WBS §5.2 EP-04 인용: "ST-04-2 — TK-04-2-1 zero-슬롯 품번 식별, TK-04-2-2 예외 리포트 출력, TK-04-2-3 단위 테스트"
> SRS REQ-FUNC-VC-003 / BR-V11 인용: "시스템은 저압·IC 모두에서 적합성 false인 품번을 스케줄 불가로 표시하고 후보 생성에서 제외해야 한다."

REF-09에서 모든 슬롯 O/X가 false인 품번(`7X375-H0020`·`28415-08400` 등)을 **후보 생성 입력 단계에서 사전 분리**. 예외 리포트로 출력하여 외주·재고 대응 권고(ASM-10). 본 Story 미통과 시 EP-05 회전 배치가 비현실적 후보를 생성할 가능성.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-04-2-1](TK-04-2-1.md) | Unschedulable 품번 식별 + 분리 서비스 | 0.3 | Backend | T-U + I | ☐ |
| [TK-04-2-2](TK-04-2-2.md) | 예외 리포트 출력 (Excel·UI 알림) | 0.2 | Backend | T-I | ☐ |
| [TK-04-2-3](TK-04-2-3.md) | 단위 테스트 + REF-09 정합 회귀 | 0.2 | QA + Backend | T-U | ☐ |

> **선행 의존**: [ST-04-1](../ST-04-1/_Story_Overview.md) (matrix.unschedulableHoseIds)
> **후행**: EP-05 (회전 배치는 본 Story 결과 제외 후 진행)

---

## Story 레벨 DoD

- [ ] **Unschedulable 자동 분리** — 후보 생성 입력 시 자동 필터
- [ ] **예외 리포트** — Excel + UI 모두 출력 (P1·P4 페르소나 확인 가능)
- [ ] **REF-09 정합** — 47품번 중 zero-slot 정확 식별 (예상 4건)
- [ ] 단위 테스트 ≥ 80%

---

## References

- **WBS**: §5.2 EP-04 ST-04-2
- **SRS REQ-FUNC**: REQ-FUNC-VC-003
- **BR**: BR-V11 (Unschedulable 사전 제외)
- **ASM-10**: 슬롯 0 품번 외주·재고 대응
- **TestPlan**: TC-VC-003
- **연관**: 선행 [ST-04-1](../ST-04-1/_Story_Overview.md), 후속 EP-05

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-04 ST-04-2 + REQ-FUNC-VC-003 + BR-V11 |
