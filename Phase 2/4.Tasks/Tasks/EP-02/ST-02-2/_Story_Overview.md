# Story Overview — [EP-02] ST-02-2 우선순위 해소 (확정 > 주간 > KD > 예상)

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-02 중복 감지 (M-02) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.1 EP-02 인용: "ST-02-2 — TK-02-2-1 우선순위 룰 엔진, TK-02-2-2 해소 audit 로그, TK-02-2-3 단위 테스트 4종 케이스"
> SRS REQ-FUNC-OC-006 인용: "시스템은 `order_type` 우선순위 **Confirmed > Weekly > KD > Forecast** 로 중복을 해소해야 한다 (BR-O01)."

본 Story는 ST-02-1에서 감지된 `DuplicateGroup`을 **명시적 우선순위 룰**로 해소한다. 영업 현장의 실제 정신: *"확정 수주(Confirmed)가 들어오면 그것이 절대 우선, 예상(Forecast)은 가장 약함."* 룰을 코드 if-else가 아닌 **enum ordinal 기반 룰 엔진**으로 구현하여 향후 정책 변경 시 enum 순서만 수정.

**Why 본 Story가 Sprint 1 핵심인가**:
- **BR-O01 정책 정확성** — 영업·생산기획 부서 합의사항
- **해소 audit 추적성** — *"왜 KD가 Forecast를 이겼는가"* 사후 검증 가능
- 본 Story 미통과 시 ST-02-1에서 감지만 하고 자동 해소 안 됨 → 사용자 수동 결정 → P1의 4.2h 부담 복원

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-02-2-1](TK-02-2-1.md) | 우선순위 룰 엔진 (OrderType enum ordinal 기반) | 0.6 | Backend | T-U + I | ☐ |
| [TK-02-2-2](TK-02-2-2.md) | 해소 audit 로그 (PrecedenceResolution 테이블) | 0.4 | Backend | T-I + I | ☐ |
| [TK-02-2-3](TK-02-2-3.md) | 4종 케이스 단위 테스트 + 회귀 슈트 | 0.4 | QA + Backend | T-U + A | ☐ |

> **선행 의존**: [TK-02-1-3](../ST-02-1/TK-02-1-3.md) (DuplicateGroup)
> **후행 차단**: EP-03 (Diff·알림 — 해소된 정본 row를 비교 대상으로 사용)

---

## Story 레벨 DoD

- [ ] **OrderType enum 순서** `FORECAST < KD < WEEKLY < CONFIRMED` 정의 (ordinal 작을수록 약함)
- [ ] **DuplicateGroup 해소**: 후보 + 기존 중 ordinal 최대인 row가 정본(winner), 나머지는 archive
- [ ] **해소 audit row** — 누가·언제·어떤 룰·승자·패자 모두 기록 (BR-X02 정합)
- [ ] **4종 케이스 단위 테스트** 모두 통과 (TC-OC-006)
- [ ] 단위 테스트 ≥ 80% 커버리지
- [ ] Sprint Review 데모: 같은 키 4종 type 동시 입력 → CONFIRMED 승리 시연

---

## References

- **WBS Story**: §5.1 EP-02 ST-02-2
- **SRS REQ-FUNC**: REQ-FUNC-OC-006
- **BR**: BR-O01 (Confirmed > Weekly > KD > Forecast)
- **PDD-01**: §4 A3 T3.2
- **TestPlan**: TC-OC-006 (우선순위 해소)
- **연관**: 선행 [ST-02-1](../ST-02-1/_Story_Overview.md), 후속 EP-03 Diff·알림

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.1 EP-02 ST-02-2 + REQ-FUNC-OC-006 + BR-O01 + TC-OC-006 |
