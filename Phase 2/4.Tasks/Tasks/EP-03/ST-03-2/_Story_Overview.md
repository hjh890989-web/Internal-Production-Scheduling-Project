# Story Overview — [EP-03] ST-03-2 Critical 태깅 (납기·수량±20%·품번)

**Sprint**: S1 | **Epic**: EP-03 Diff·알림 (M-03) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.1 EP-03 인용: "ST-03-2 — TK-03-2-1 Critical 분류기, TK-03-2-2 단위 테스트, TK-03-2-3 BR-O02 정합"
> SRS REQ-FUNC-OC-008 인용: "시스템은 다음 조건에서 Critical로 분류: (a) `delivery_date` 변경 (b) `qty` ±20% 이상 변경 (c) `hose_id` 변경. False Negative 0."

본 Story는 ST-03-1의 `RowDiff(type=MODIFIED)` 셋에 **Critical/Normal severity 태깅**을 부여한다. BR-O02 정합. **False Negative 0** — Critical을 놓치는 일이 절대 없어야 한다. INT-1 사건(300개 납기 지연)이 본 분류 실패에서 비롯됨.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-03-2-1](TK-03-2-1.md) | Critical 분류기 (zero-false-negative) | 0.6 | Backend | T-U + A | ☐ |
| [TK-03-2-2](TK-03-2-2.md) | 단위 테스트 + BR-O02 회귀 (100건 Critical 케이스) | 0.5 | QA + Backend | T-U + A | ☐ |
| [TK-03-2-3](TK-03-2-3.md) | BR-O02 정합 + ConfigProperties 임계치 외부화 | 0.3 | Backend | I + T-U | ☐ |

> **선행 의존**: [ST-03-1](../ST-03-1/_Story_Overview.md) (RowDiff)
> **후행 차단**: ST-03-3 (알림은 severity 기반으로 채널·SLA 차별)

---

## Story 레벨 DoD

- [ ] BR-O02 정합: 납기 변경·수량 ±20%·품번 변경 → 모두 Critical
- [ ] **False Negative 0** — 회귀 100건 Critical 후보 100% 정확 태깅
- [ ] False Positive ≤ 5% (정상 변경이 Critical로 잘못 분류되는 경우 최소화)
- [ ] **임계치 외부화** (±20% 등) — application.yaml에서 조정 가능
- [ ] order_change.severity 컬럼 UPDATE
- [ ] 단위 테스트 ≥ 80% 커버리지

---

## References

- **WBS**: §5.1 EP-03 ST-03-2
- **SRS REQ-FUNC**: REQ-FUNC-OC-008
- **BR**: BR-O02 (Critical 분류 기준)
- **PDD-01**: §4 A3 T3.4
- **TestPlan**: TC-OC-008 (Critical 태깅 zero-FN)
- **연관**: 선행 [ST-03-1](../ST-03-1/_Story_Overview.md), 후속 [ST-03-3](../ST-03-3/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.1 EP-03 ST-03-2 + REQ-FUNC-OC-008 + BR-O02 + TC-OC-008 |
