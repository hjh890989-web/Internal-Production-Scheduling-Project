# Story Overview — [EP-21] ST-21-1 VC_CONSTRAINT K/L 컬럼 + 좌/우 제약

**Sprint**: S2 | **Epic**: EP-21 v1.4 신규 Must | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-21 ST-21-1: "TK-21-1-1 ALTER TABLE 마이그레이션 (`lp_left_setting`·`lp_right_setting` CHAR(1) CHECK), TK-21-1-2 RuleEngine 좌/우 검증 함수, TK-21-1-3 `28421-2M800`/`28422-2M800` 회귀 통과"
> SRS REQ-FUNC-VC-021 / BR-V15·V16: "성형공정_제약조건.xlsx K열(좌) / L열(우) 셋팅 값을 강제 — 슬롯 셋팅 위치와 일치하지 않으면 배치 불가"

본 Story는 **v1.4 마스터 분석**에서 발견된 좌/우 셋팅 제약을 데이터·로직 양쪽에 반영. K/L열은 가류기 슬롯별 좌측·우측 셋팅 호환성을 'O'/'X'로 표시하며, RuleEngine은 슬롯 셋팅 방향이 품번 요구와 일치하는지 검증해야 한다.

**Why Must (Sprint 2 핵심)**:
- 잘못 배치 시 가류 불량 직결 (현장 실측 1주 평균 6건)
- ST-21-4 (28422-2M800 우측, 28421-2M800 좌측) 선행
- BR-V15·V16 hard 제약

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-21-1-1](TK-21-1-1.md) | VC_CONSTRAINT ALTER TABLE 마이그레이션 (lp_left/right_setting) | 0.7 | Backend | T-U + I | ☐ |
| [TK-21-1-2](TK-21-1-2.md) | RuleEngine 좌/우 검증 함수 | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-21-1-3](TK-21-1-3.md) | `28421-2M800`/`28422-2M800` 회귀 통과 | 0.6 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-04 ST-04-1](../../EP-04/ST-04-1/_Story_Overview.md), [EP-99 ST-99-1](../../EP-99/ST-99-1/_Story_Overview.md)
> **후행**: ST-21-4 (품번 단위 ≤2 결합)

---

## Story 레벨 DoD

- [ ] `master.VC_CONSTRAINT` 테이블 + `lp_left_setting CHAR(1) CHECK ('O','X')`, `lp_right_setting CHAR(1) CHECK ('O','X')` 컬럼 추가
- [ ] 47품번 K/L열 seed 데이터 INSERT (ST-99-1 검증 통과 데이터)
- [ ] RuleEngine `validateLeftRight(hose, machine, slot, side)` 함수
- [ ] `28421-2M800` 좌측 셋팅·`28422-2M800` 우측 셋팅 회귀 통과
- [ ] 단위 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21 ST-21-1
- **PDD-02 v1.1**: BR-V15·V16
- **SRS REQ-FUNC**: REQ-FUNC-VC-021
- **TestPlan**: TC-VC-021
- **선행**: EP-04 (슬롯 O/X), EP-99 (마스터 K/L 검증)
- **후행**: ST-21-4

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 ST-21-1 |
