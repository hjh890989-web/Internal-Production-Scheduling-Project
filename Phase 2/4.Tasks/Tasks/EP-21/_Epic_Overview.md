# Epic Overview — [EP-21] v1.4 신규 Must: 좌/우·호기·품번앵글상한·규격<7 제약

**Sprint**: S2 (성형 핵심) | **Priority**: Must ⭐ | **SP 합계**: 13 | **PD 추정**: ~9.1 PD

---

## Epic 목적

> WBS §5 EP-21 인용: "v1.4 신규 VC 요구사항 — REQ-FUNC-VC-021/024/025/026/027 Must"
> SRS v1.4 §17.5: "성형공정_제약조건.xlsx K/L열(좌/우 셋팅) + 28422-08HA0/28422-2M800/28421-2M800 룰 + 압출공정_제약조건.xlsx B열(규격) cross-master 제약을 RuleEngine이 강제해야 한다."

본 Epic은 **2026-Q1 v1.4 마스터 분석 결과** 발견된 5종 신규 제약을 Sprint 2 핵심으로 통합:
- **REQ-FUNC-VC-021 (BR-V15·V16)**: 슬롯 좌/우 셋팅 강제 — 잘못 배치 시 가류 불량
- **REQ-FUNC-VC-024 (BR-V14, 28422-08HA0)**: LP-01 단일 셋팅 호기 (동시 다중 슬롯 차단)
- **REQ-FUNC-VC-025·026 (BR-V15·V16)**: `28422-2M800` 우측·≤2 / `28421-2M800` 좌측·≤2
- **REQ-FUNC-VC-027 (BR-V17, ADR-017)**: 규격<7 품번 가류기당 앵글 ≤4 (cross-master VIEW)

**Why Sprint 2 핵심**:
- 정확성 R-V01·V05·V08 직접 차단 (회전 배치 무효화 방지)
- ST-99-1·99-2 (Phase 0 마스터 검증) 결과 직접 사용
- Sprint 2 DoD: "v1.4 좌/우·호기·품번앵글상한·규격<7 위반 모두 0건"

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-21-1](ST-21-1/_Story_Overview.md) | VC_CONSTRAINT K/L 컬럼 + 좌/우 제약 | 3 | ~2.1 | T-U + T-I | ☐ |
| [ST-21-2](ST-21-2/_Story_Overview.md) | VC_HOSE_RULE 마스터 테이블 + 호기·앵글 상한 | 3 | ~2.1 | T-U + T-I | ☐ |
| [ST-21-3](ST-21-3/_Story_Overview.md) | `28422-08HA0` LP-01 단일 셋팅 | 2 | ~1.4 | T-U + T-I + A | ☐ |
| [ST-21-4](ST-21-4/_Story_Overview.md) | `28422-2M800` 우측·≤2 + `28421-2M800` 좌측·≤2 | 2 | ~1.4 | T-U + T-I + A | ☐ |
| [ST-21-5](ST-21-5/_Story_Overview.md) | 규격<7 가류기당 앵글 ≤4 (cross-master) | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **VC_CONSTRAINT 테이블** `lp_left_setting`·`lp_right_setting` CHAR(1) CHECK 컬럼 추가 + 47품번 seed
- [ ] **VC_HOSE_RULE 마스터 테이블** (`machine_pin`·`max_concurrent_slots`·`side_lock`·`lp_only`) + LISTEN/NOTIFY 캐시 무효화
- [ ] **`v_product_with_spec` VIEW** + Caffeine 캐시 + EX_CONSTRAINT LISTEN/NOTIFY
- [ ] **RuleEngine 5종 신규 함수** (좌/우 검증·호기 핀·동시 슬롯 상한·품번 앵글 상한·규격<7 cap) 통합
- [ ] **회귀 100건 위반 0건** — 5종 제약 모두 통과 (TC-VC-021·024·025·026·027)
- [ ] **마스터 정합성**: ST-99-1·99-2 검증 통과 (선행 완료)

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21
- **PDD-02 v1.1**: BR-V14·V15·V16·V17
- **SRS REQ-FUNC**: REQ-FUNC-VC-021·024·025·026·027
- **SAD**: ADR-017 (cross-master VIEW 패턴)
- **TestPlan**: TC-VC-021·024·025·026·027
- **선행**: EP-04 (슬롯 O/X), EP-99 (마스터 검증)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 + v1.4 신규 VC 5종 |
