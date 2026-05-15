# Story Overview — [EP-03] ST-03-1 이전 버전 Diff 알고리즘

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-03 Diff·알림 (M-03) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.1 EP-03 인용: "ST-03-1 — TK-03-1-1 row-level diff 엔진, TK-03-1-2 100% 변형 회귀 통과, TK-03-1-3 diff 결과 데이터 모델"
> SRS REQ-FUNC-OC-007 인용: "시스템은 후보 Import와 이전 Master 버전 간 row-level diff를 계산하여 각 row를 신규 / 수정(필드별) / 삭제로 분류해야 한다. 검출 정확도 100%."

본 Story는 EP-02 해소가 끝난 정본(Resolution.winner) 셋과 **이전 마스터 버전(master_version - 1)** 을 비교하여 **신규·수정·삭제**를 row 단위 분류한다. INT-1 시각: P1이 매주 월요일 변경을 파악하려고 VLOOKUP에 4.2시간 쓰는 핵심 단계의 자동화. P3·P2 알림(ST-03-3)의 입력원.

**왜 본 Story가 Sprint 1 핵심인가**:
- **REQ-FUNC-OC-007 100% diff 정확도** — Sprint 1 DoD 강제
- **EXP-1 베이스라인 비교의 가시 지표** — diff 자동화 시간 측정 직접 효과
- **Critical 분류(ST-03-2)와 알림(ST-03-3)의 입력원** — 본 Story 미통과 시 EP-03 전체 차단

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-03-1-1](TK-03-1-1.md) | row-level diff 엔진 (좌·우 join + 필드별 비교) | 1.0 | Backend | T-U + A | ☐ |
| [TK-03-1-2](TK-03-1-2.md) | 100% 변형 회귀 (의도적 변형 100건 검증) | 0.6 | QA + Backend | T-I + A | ☐ |
| [TK-03-1-3](TK-03-1-3.md) | DiffResult 데이터 모델 + 영속성 | 0.5 | Backend | T-U + I | ☐ |

> **선행 의존**: [ST-02-2](../../EP-02/ST-02-2/_Story_Overview.md) (Resolution → 정본 row)
> **후행 차단**: ST-03-2 (Critical 태깅), ST-03-3 (알림)

---

## Story 레벨 DoD

- [ ] **신규·수정·삭제 3 분류** 정확 100% (TC-OC-007)
- [ ] **수정 분류는 필드별 before/after** 포함 (납기·수량·품번·거래처)
- [ ] **빈 마스터** (master_version=0)일 때 모든 row가 NEW로 분류
- [ ] **동일 마스터** (변경 0건)일 때 모든 row가 UNCHANGED
- [ ] **DiffResult 영속화** + 임의 시점 재조회 가능 (REQ-FUNC-OC-014 부속)
- [ ] 단위 + 통합 테스트 ≥ 80% 커버리지

---

## References

- **WBS**: §5.1 EP-03 ST-03-1
- **SRS REQ-FUNC**: REQ-FUNC-OC-007
- **SAD**: §6.2.5 ORDER_CHANGE 엔티티
- **PDD-01**: §4 A3 T3.3
- **TestPlan**: TC-OC-007 (100% diff 정확도)
- **연관**: 선행 [ST-02-2](../../EP-02/ST-02-2/_Story_Overview.md), 후속 [ST-03-2](../ST-03-2/_Story_Overview.md), [ST-03-3](../ST-03-3/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.1 EP-03 ST-03-1 + REQ-FUNC-OC-007 + TC-OC-007 |
