# Story Overview — [EP-04] ST-04-1 슬롯 적합성 매트릭스 빌드

**Sprint**: S2 (성형 핵심) | **Epic**: EP-04 슬롯 O/X 검증 (M-04) | **Priority**: Must
**SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Story 목적

> WBS §5.2 EP-04 인용: "ST-04-1 — TK-04-1-1 VC_CONSTRAINT 엔티티(G~J·M~O 컬럼) + Repository, TK-04-1-2 매트릭스 빌드 서비스 (≤1초 재구축), TK-04-1-3 /api/v1/master/compat 엔드포인트, TK-04-1-4 회귀 100건 위반 0 검증"
> SRS REQ-FUNC-VC-001 인용: "시스템은 VC_CONSTRAINT의 컬럼(lp_slot_top, lp_slot_upmid, lp_slot_lowmid, lp_slot_bot, ic_slot_top, ic_slot_mid, ic_slot_bot)으로부터 (hose_id × 가류기 유형 × 슬롯 위치) 적합성 매트릭스를 구축해야 한다."

본 Story는 **성형 스케줄링의 핵심 의사결정 데이터**인 슬롯 O/X 적합성 매트릭스를 구축한다. REF-09(`성형공정_제약조건.xlsx`)의 47품번 × 7 슬롯위치(저압 4 + IC 3) = 329개 셀의 O/X를 인메모리 매트릭스로 변환·캐시. INT-4 사건: *"IC 가류기에 저압 전용 제품을 넣어서 반장한테 혼났어요"*(최민혁 대리) — 본 매트릭스가 이런 사고를 시스템적으로 차단.

**Why 본 Story가 Sprint 2 핵심인가**:
- **모든 후속 EP-05·06·21의 입력원** — 매트릭스 없이 회전 배치 불가
- **REQ-FUNC-VC-002 회귀 100건 위반 0건** — Sprint 2 DoD 가시 지표
- **NFR-PER 매트릭스 빌드 ≤1초** — 마스터 데이터 변경 시 즉시 반영
- **EXP-3 사용자 가치** — P4(최민혁 대리)가 P1 부재 시에도 슬롯 사고 0건

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-04-1-1](TK-04-1-1.md) | VC_CONSTRAINT 엔티티 (G~J·M~O bool 컬럼) + Repository | 1.0 | Backend + DBA | T-I + I | ☐ |
| [TK-04-1-2](TK-04-1-2.md) | SlotCompatibilityMatrix 빌드 서비스 (≤1초 재구축) | 1.0 | Backend | T-U + T-L | ☐ |
| [TK-04-1-3](TK-04-1-3.md) | `GET /api/v1/master/compat` 엔드포인트 + 캐싱 | 0.5 | Backend | T-I + I | ☐ |
| [TK-04-1-4](TK-04-1-4.md) | 100건 회귀 슬롯 위반 0 검증 + DS-VC-CONSTRAINT-47 | 1.0 | QA + Backend | T-I + A | ☐ |

> **선행 의존**: [ST-99-1](../../EP-99/ST-99-1/_Story_Overview.md) (성형 마스터 정비), [ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md) (PostgreSQL)
> **후행 차단**: ST-04-2 (Unschedulable 분리 — 본 매트릭스 사용), ST-04-3 (UI 드래그 가드), EP-05·06·21 (모든 후속)
> **병렬 가능**: EP-05 ST-05-1 (회전 모델은 독립)

---

## Story 레벨 DoD

- [ ] `VC_CONSTRAINT` 47품번 마스터 데이터 적재 (Phase 0 EP-99 출력)
- [ ] **(hose_id × machine_type × slot_position) 적합성 매트릭스** 빌드
- [ ] **마스터 변경 시 ≤1초 재구축** (NFR-PER REQ-FUNC-VC-001)
- [ ] `GET /api/v1/master/compat` 엔드포인트 정상 (p95 ≤500ms)
- [ ] **100 회귀 배치에서 슬롯 O/X 위반 0건** (TC-VC-002)
- [ ] **47품번 × 7 슬롯 = 329 셀** 모든 적합성 정확 (REF-09 매뉴얼 검증과 일치)
- [ ] 단위 + 통합 테스트 ≥ 80% 커버리지

---

## References (공통)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-04 ST-04-1
- **SAD**:
  - §4 컴포넌트 — `RuleMod` 슬롯 O/X 검증
  - §5.3 데이터 — Redis 캐시 (슬롯 매트릭스 1시간 TTL + LISTEN/NOTIFY 무효화)
  - §6.2.2 VC_CONSTRAINT 컬럼 spec
  - §10 ADR-010 (PostgreSQL JSONB·LISTEN/NOTIFY)
- **SRS REQ-FUNC**:
  - **REQ-FUNC-VC-001** (매트릭스 ≤1초 재구축)
  - **REQ-FUNC-VC-002** (슬롯 위반 거부, 회귀 100건 위반 0)
- **PDD-02 v1.2**: `Phase 2/1.PDD/2.process_vulcanization_scheduling_Opus.md` §4 A3 슬롯 적합성 매트릭스
- **REF-09**: `Phase 1/2.Raw Materials/Vulcanization/성형공정_제약조건.xlsx` — 47품번 마스터
- **REF-11**: `Phase 1/2.Raw Materials/Vulcanization/클로드_성형_프롬프트.docx`
- **TestPlan**: TC-VC-001 (≤1초 재구축), TC-VC-002 (100건 위반 0)
- **BR**: BR-V01 (슬롯 적합성 강제)
- **연관 Story**:
  - 선행: [ST-99-1](../../EP-99/ST-99-1/_Story_Overview.md) (마스터 데이터 정비)
  - 후속: [ST-04-2](../ST-04-2/_Story_Overview.md), [ST-04-3](../ST-04-3/_Story_Overview.md), Sprint 2 EP-05·06·21

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | EP-04 ST-04-1 초안 — Sprint 2 진입 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS v1.2 §5.2 EP-04 ST-04-1 + SAD §6.2.2 + REQ-FUNC-VC-001·002 + REF-09 + TC-VC-001·002 |
