# Story Overview — [EP-05] ST-05-1 회전 단위 용량 모델 (18 회전/대)

**Sprint**: S2 | **Epic**: EP-05 회전수 배치 (M-05) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-05 인용: "ST-05-1 — TK-05-1-1 회전 도메인 모델, TK-05-1-2 일일 capa 계산 (저압 72 + IC 18), TK-05-1-3 단위 테스트"
> SRS REQ-FUNC-VC-005 / BR-V04·V05 인용: "시스템은 1일 가류기당 18 회전(주간 8 + 야간 10)으로 모델링하여 일일 총 용량 = 저압 fleet 72 회전 + IC 18 회전으로 적용해야 한다. 스케줄 출력 키에 `(date, rotation_no ∈ 1..18, machine_id, slot_position)` 포함."

본 Story는 **EP-05 회전 배치의 도메인 기반**. ADR-005 정신 — "회전수가 PK, 시간이 아니다". 저압 4대(LP-01~LP-04) + IC 1대(IC-01) × 회전(1~18) × 슬롯(저압 8 + IC 6)으로 표현되는 시간·자원 격자를 자료 구조로 구축. EP-05·06·21의 모든 후속 배치가 본 격자 위에서 동작.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-05-1-1](TK-05-1-1.md) | 회전 도메인 모델 (date·rotation 1~18·machine·slot) | 1.0 | Backend | T-U + I | ☐ |
| [TK-05-1-2](TK-05-1-2.md) | 일일 capa 계산 (저압 72 + IC 18) + CapacityLedger | 0.6 | Backend | T-U + T-L | ☐ |
| [TK-05-1-3](TK-05-1-3.md) | 단위 테스트 (BR-V04·V05 진리표) | 0.5 | QA + Backend | T-U + A | ☐ |

> **선행 의존**: [TK-04-1-2](../../EP-04/ST-04-1/TK-04-1-2.md) (SlotCompatibilityMatrix)
> **후행 차단**: ST-05-2 (yield 계산), ST-05-3 (회전 배치 알고리즘), EP-06 (D-2 역산)

---

## Story 레벨 DoD

- [ ] **VC_MACHINE 마스터 테이블** — 5 가류기 (LP-01·LP-02·LP-03·LP-04·IC-01) seed
- [ ] **회전 도메인 키** `(date, machine_id, rotation_no ∈ 1..18, slot_position)` 정확
- [ ] **일일 capa 계산**: 저압 4대 × 18회전 = 72 + IC 1대 × 18회전 = 18
- [ ] **`CapacityLedger`** 가용·예약·확정 회전 추적
- [ ] BR-V04·V05 진리표 단위 테스트 통과
- [ ] 단위 테스트 ≥ 80% 커버리지

---

## References

- **WBS Story**: §5.2 EP-05 ST-05-1
- **SAD**: §6.2.6 VC_MACHINE · §6.2.7 VC_SCHEDULE (rotation_no 1~18) · §10 ADR-005
- **SRS REQ-FUNC**: REQ-FUNC-VC-005
- **BR**: BR-V04 (주간 8 + 야간 10 = 18), BR-V05 (저압 4 + IC 1)
- **PDD-02 v1.2**: §4 A4 T4.1 (일일 가용 capa)
- **REF-11**: `클로드_성형_프롬프트.docx` (회전 모델 출처)
- **TestPlan**: TC-VC-005 (회전 단위 키)
- **연관**: 선행 [TK-04-1-2](../../EP-04/ST-04-1/TK-04-1-2.md), 후속 [ST-05-2](../ST-05-2/_Story_Overview.md), [ST-05-3](../ST-05-3/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-05 ST-05-1 + ADR-005 + BR-V04·V05 + REQ-FUNC-VC-005 |
