# Story Overview — [EP-05] ST-05-2 회전당 yield + 앵글 가용량 검증

**Sprint**: S2 | **Epic**: EP-05 회전수 배치 (M-05) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-05 인용: "ST-05-2 — TK-05-2-1 yield 계산, TK-05-2-2 앵글 capa 검증, TK-05-2-3 stress 회귀"
> SRS REQ-FUNC-VC-006 / BR-V03 인용: "시스템은 회전당 yield를 `합금형(composite_count) × 앵글당 금형수(active 기계 유형의 molds_per_angle)`로 계산해야 한다."
> SRS REQ-FUNC-VC-007 / BR-V06 인용: "시스템은 hose_id를 동시 점유하는 슬롯 총 수가 앵글 보유량(`lp_angle_qty` 또는 `ic_angle_qty`)을 초과하지 않도록 보장해야 한다."

본 Story는 **회전·슬롯 단위 생산량(yield)과 앵글 자원 제약**을 정량화. ST-05-3 회전 배치 알고리즘이 본 계산을 입력으로 받아 정확한 Q_required 충족 + 앵글 과초과 방지.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-05-2-1](TK-05-2-1.md) | 회전당 yield 계산 (합금형 × 앵글당금형수, BR-V03) | 0.7 | Backend | T-U + A | ☐ |
| [TK-05-2-2](TK-05-2-2.md) | 앵글 capa 검증기 (BR-V06, lp/ic_angle_qty 상한) | 0.7 | Backend | T-U + I | ☐ |
| [TK-05-2-3](TK-05-2-3.md) | Stress 회귀 (앵글 과초과 0건, 1000회 무작위) | 0.7 | QA + Backend | T-S + A | ☐ |

> **선행 의존**: [ST-04-1](../../EP-04/ST-04-1/_Story_Overview.md) (VC_CONSTRAINT 엔티티 — `composite_count`, `lp_molds_per_angle`, `lp_angle_qty` 등)
> **후행 차단**: ST-05-3 (회전 배치 알고리즘은 yield + 앵글 capa 모두 사용)

---

## Story 레벨 DoD

- [ ] **회전당 yield = composite_count × molds_per_angle (machine_type별)** 정확 (BR-V03)
- [ ] **47품번 모두 yield 계산 검증** (REF-09 데이터 기반)
- [ ] **앵글 capa**: 동시 점유 슬롯 수 ≤ lp_angle_qty 또는 ic_angle_qty (BR-V06)
- [ ] **앵글 교체 페널티 1회전 손실** (BR-V07 정신 — ST-05-3에서 통합)
- [ ] **1000회 stress 회귀** 앵글 과초과 0건
- [ ] 단위 + stress 테스트 ≥ 80% 커버리지

---

## References

- **WBS Story**: §5.2 EP-05 ST-05-2
- **SAD**: §6.2.2 VC_CONSTRAINT 컬럼 spec (E·F·K·L 컬럼이 출처)
- **SRS REQ-FUNC**: REQ-FUNC-VC-006·007
- **BR**: BR-V03 (yield 수식), BR-V06 (앵글 capa)
- **PDD-02 v1.2**: §4 A4 T4.2·T4.5
- **REF-09**: `성형공정_제약조건.xlsx` E·F·K·L 컬럼
- **REF-11**: `클로드_성형_프롬프트.docx` (앵글 정의)
- **TestPlan**: TC-VC-006 (yield 단위), TC-VC-007 (앵글 capa)
- **연관**: 선행 [TK-04-1-1](../../EP-04/ST-04-1/TK-04-1-1.md) (VcConstraint 엔티티), 후속 [ST-05-3](../ST-05-3/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-05 ST-05-2 + BR-V03·V06 + REQ-FUNC-VC-006·007 |
