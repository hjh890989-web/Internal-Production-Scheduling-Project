# Story Overview — [EP-99] ST-99-1 성형 마스터 K/L열 + 품번별 호기·앵글 상한 정합성 검증

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-99 마스터 데이터 정비 (선행 작업) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD (3 SP × 0.7 PD)

---

## Story 목적

> WBS §5.1 EP-99 인용: "Sprint 1 진입 전 인프라·마스터·CI/CD 셋업. 별도 Sprint로 분리하여 개발 흐름 차단 방지."

본 Story는 **v1.4 신규 성형 제약 4건(BR-V14·V15·V16 + REQ-FUNC-VC-021/024/025/026)이 강제하는 마스터 데이터의 무결성을 Phase 1.0 개발 진입 전에 사전 검증**한다. 구체적으로:

1. 성형 마스터(`성형공정_제약조건.xlsx`) **K(좌측셋팅)·L(우측셋팅) 컬럼**이 47개 품번 모두 `o`/`x` 값으로 정합한지 검증
2. v1.4에서 명시된 **품번별 특수 제약 3종**(`28422-08HA0`·`28422-2M800`·`28421-2M800`)이 마스터 데이터·`VC_HOSE_RULE` 시드와 cross-check 일치하는지 확인
3. 마스터 무결성을 지속적으로 보장할 **회귀 SQL** 작성 (Phase 1.0 이후 dual-review 운영 시 활용)

**Why 본 Story가 Phase 0 첫 작업인가**:
- 본 Story가 통과하지 못하면 EP-04 (슬롯 O/X)·EP-05 (회전 배치)·EP-21 (v1.4 신규 좌/우·호기·앵글 상한) 등 **Sprint 2 핵심 Epic 전체가 차단**됨 (WBS §12 의존성 DAG)
- SRS-RSK-001 (마스터 데이터 부정확) + R-X01 (마스터 부정확) 직접 완화 (WBS §13.1)
- SAD-RSK-010 (마스터 K/L·B열 무결성 누락) 완화

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-99-1-1](TK-99-1-1.md) | `성형공정_제약조건.xlsx` K/L열 47품번 무결성 검증 | 0.7 | QA + DBA | I + T-U | ☐ |
| [TK-99-1-2](TK-99-1-2.md) | `28422-08HA0`/`28422-2M800`/`28421-2M800` 룰 명세 cross-check | 0.7 | QA + 백엔드 | I + A | ☐ |
| [TK-99-1-3](TK-99-1-3.md) | 마스터 무결성 회귀 SQL 작성 (Phase 1.0 이후 지속 운영용) | 0.7 | DBA | T-U + A | ☐ |

> **선행 의존**: 없음 (Phase 0 시작 작업)
> **후행 차단**: ST-99-2 · EP-00 진행 가능 (병렬) / EP-04 · EP-21 (S2)는 본 Story 종료 후에만 시작

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **47품번 K/L열 모두 `o`/`x` 정합** (NULL·기타 값 0건) — TK-99-1-1
- [ ] **품번 단위 특수 제약 3종 cross-check 완료** — TK-99-1-2:
  - `28422-08HA0`: K=? L=? lp_only=TRUE machine_pin=LP-01 max_concurrent_slots=1
  - `28422-2M800`: K=`x` L=`o` (우측 only) max_concurrent_slots=2
  - `28421-2M800`: K=`o` L=`x` (좌측 only) max_concurrent_slots=2
- [ ] **회귀 SQL 4종 작성·검증** — TK-99-1-3 (CHECK 위반·NULL·중복·미정의 호기)
- [ ] **dual-review 사인오프** (P1 김정훈 주임 + STK-08 IT lead) — BR-X05
- [ ] **As-Is 베이스라인 일자 기록** (Phase 0 시점 정합 상태 스냅샷)
- [ ] Sprint Review 데모 시연 — 마스터 47품번 100% 정합 + 회귀 SQL 통과 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-99 ST-99-1
- **SRS REQ-FUNC**: `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` §4.1.2
  - REQ-FUNC-VC-021 (좌/우 슬롯 측면)
  - REQ-FUNC-VC-024 (`28422-08HA0` 호기 단일 셋팅)
  - REQ-FUNC-VC-025 (`28422-2M800` 우측 ≤2)
  - REQ-FUNC-VC-026 (`28421-2M800` 좌측 ≤2)
- **PDD BR**: `Phase 2/1.PDD/2.process_vulcanization_scheduling_Opus.md` §9
  - BR-V14·BR-V15·BR-V16 (품번 단위 특수 제약 3종)
- **SAD ADR / DDL**: `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md`
  - §6.1.1a `master.VC_CONSTRAINT.lp_left_setting`·`lp_right_setting` 컬럼 + `master.VC_HOSE_RULE` 테이블 DDL
- **SRS-RSK**: SRS §1.7.1 SRS-RSK-001 "마스터 데이터 부정확 → 비현실적 스케줄"
- **마스터 데이터 원본**: `Phase 1/2.Raw Materials/Vulcanization/성형공정_제약조건.xlsx`
  - 47품번 × 22열 (A=HOSE·B=사양·C=금형보유수량·D=합금형·E=저압가류기·F=저압앵글보유수량·G~J=저압슬롯1~4·**K=좌측셋팅·L=우측셋팅(v1.1 신설)**·M=IC가류기·N=IC앵글보유수량·O~Q=IC슬롯1~3)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | 초안 작성 (Phase 2/4.Tasks/Sprints 외 첫 시범 — Task 기반 분해 패턴 적용) |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.1 EP-99 ST-99-1 기반. Task 기반 분해 패턴 첫 시범 적용 (`0.Prompt/20260515_C_Phase2_Task_Extraction_Task_Based_v1.md` 따름) |
