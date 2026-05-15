# Story Overview — [EP-13] ST-13-3 일말 교체 경계 + DO-04 영업일 키 출력

**Sprint**: S4 | **Epic**: EP-13 당일 락 | **Priority**: Must
**SP 합계**: 1 | **PD 추정**: ~0.7 PD

---

## Story 목적

> WBS §7 EP-13 ST-13-3: "TK-13-3-1 DO-04 출력 형식 변경 (영업일 경계 키), TK-13-3-2 audit 검증, TK-13-3-3 단위 테스트"
> SRS REQ-FUNC-VC-013 / BR-V07: "일중 락이 해제되는 시점은 영업일 경계 (다음 영업일 00:00). DO-04 출력 형식에 영업일 경계 키를 명시."

본 Story는 DO-04 (현장 작업지시서) 출력에 영업일 경계 표시 — `YYYY-MM-DD_END` 형식. 작업자가 "다음 영업일 시작 전까지 본 셋팅 유지" 알 수 있도록. audit 측 영업일 키 검증.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-13-3-1](TK-13-3-1.md) | DO-04 출력 형식 변경 (영업일 경계 키) | 0.3 | Backend | T-U + T-I | ☐ |
| [TK-13-3-2](TK-13-3-2.md) | audit 영업일 키 검증 | 0.2 | QA | T-I | ☐ |
| [TK-13-3-3](TK-13-3-3.md) | 단위 테스트 (영업일 경계 매트릭스) | 0.2 | Backend | T-U | ☐ |

> **선행**: [ST-13-2](../ST-13-2/_Story_Overview.md), [EP-06](../../EP-06/) (캘린더)
> **후행**: ST-13-4

---

## Story 레벨 DoD

- [ ] **DO-04 영업일 경계 키**: `YYYY-MM-DD_END` (월요일 → "2026-03-02_END" = 화 00:00까지)
- [ ] **`BusinessDayBoundaryFormatter`** — LocalDate → 키 변환
- [ ] **audit row의 reason** 필드에 영업일 키 포함 (override 시)
- [ ] **단위 테스트** — 월~금 + 연휴 직전 매트릭스
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-13 ST-13-3
- **SRS REQ-FUNC**: REQ-FUNC-VC-013
- **BR**: BR-V07

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
