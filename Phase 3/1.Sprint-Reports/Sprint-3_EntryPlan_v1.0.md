# Sprint 3 진입 계획 (압출 핵심)

**Sprint**: S3 | **목표 기간**: 2026-05-22 ~ (2주, AI 가속 시 ~3일) | **상태**: 🔄 진입 게이트
**작성**: 2026-05-22 | **상위 참조**: [Sprint-2_Completion_v1.0.md](Sprint-2_Completion_v1.0.md) §11·12, [WBS v1.2 §6](../../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)

> Sprint 2 (성형 가류 6 Epic 완결) 종료 직후 진입. 46 commit · 40/40 Task · Modulith verify 0 위반 · 회귀 100%.
> Sprint 3 = **압출 (EX) 스케줄링 핵심** — BR-E01~E11 + 4-shift × 75% 효율 + yield 수식 + 셋팅 그룹핑.

---

## 1. Sprint 3 목표 (PDD-MASTER v1.7 + SRS v1.5 § EX)

- **압출 D-1 자동 역산** (M-07) — `vc_date − 1 working day` (BR-E01) — EP-06 WorkingCalendar 재사용.
- **압출 수식** (M-08) — 4-shift (주간전반·후반·야간전반·후반) × 75% 효율 + `yield = floor(speed × min × 1000 / length)` + `Q_ext = max(0, Q_vc + target_stock - current_stock)`.
- **`29673-2R060` BR-E05 reference case = 2,531** — Sprint 3 DoD 단일 가장 중요한 검증.
- **압출 셋팅 그룹핑** (M-09) — shift 내 셋업 0건 + 셋팅 그룹 1~8 동시생산.
- **압출 검증 게이트** (EP-EX11) — 누적 yield ≥ Q_ext + shift capa + 후보당 p95 ≤ 2초.
- **압출 충돌 대안** (EP-EX12) — EARLIER_START · NIGHT_SECOND_BOOST · VC_DATE_NEGOTIATE · OUTSOURCE ≥ 3 distinct.
- **EX_CONSTRAINT 풀 확장** — Sprint 2 V016 최소(spec·angle) 에서 압출 속도·길이·다이·라인 등 추가.

---

## 2. Sprint 3 Epic·SP 매트릭스

| Epic | 제목 | SP | 의존 (선행) | 핵심 산출 |
|---|---|:--:|---|---|
| **EP-07** ⭐ | 압출 D-1 자동 역산 | 5 | EP-06 (WorkingCalendar) | `ExDeadlineCalculator` + `vc.confirmed` 이벤트 구독 + `due_date` 자동 계산 |
| **EP-08** ⭐⭐ | 압출 수식 (yield + Q_ext + 4-shift) | 8 | EP-07 | `ExYieldCalculator` + 4-shift × 75% + BR-E05 reference 2,531 |
| **EP-09** | 압출 셋팅 그룹핑 (1~8) | 5 | EP-08 | `ShiftGroupingService` + 셋업 0건 보장 + 4주 회귀 |
| **EP-EX11** | 압출 검증 게이트 (p95 ≤ 2초) | 2 | EP-09 | `ExGateValidator` + 누적 yield ≥ Q_ext + shift capa |
| **EP-EX12** | 압출 충돌 대안 (≥ 3 distinct) | 2 | EP-EX11 | `ExAlternativeGenerator` (4 base 대안) |
| **EP-VC16** ST-VC16 carry-over | On-Demand 검사 endpoint (Sprint 2 완료, S3 미연장) | — | — | 완료됨 (TK-VC16-1-3) |
| **EP-EX13** carry-over | `vc.changed` 자동 트리거 | 3 | EP-10 (Sprint 4) | Sprint 3 시작 + S4 완료 |

**합계**: **~22 SP** (Sprint 3 capacity 30 PD = 50 SP velocity 기준 · ~44% 활용).
EP-07 → EP-08 → EP-09 → EP-EX11 → EP-EX12 가 critical path (22 SP).

EP-EX13 (3 SP) carry-over — Sprint 4 EP-10 (Confirmed 상태) 완료 후 활성. Sprint 3 진입만 (기반 구조).

---

## 3. 의존성 그래프

```
Sprint 2 (성형 가류)
       │
       └──► EP-07 (압출 D-1 역산) ⭐
              │ (WorkingCalendar + vc.confirmed 이벤트)
              │
              └──► EP-08 (압출 수식) ⭐⭐
                     │ (BR-E05 reference = 2,531)
                     │
                     └──► EP-09 (셋팅 그룹핑)
                            │
                            └──► EP-EX11 (검증 게이트)
                                   │
                                   └──► EP-EX12 (충돌 대안)

EP-10 (Sprint 4 Confirmed) ─────► EP-EX13 (자동 재계산 carry-over)
```

Critical Path: **EP-07 → EP-08 → EP-09 → EP-EX11 → EP-EX12** (~22 SP, ~16 PD).

---

## 4. 권장 진행 순서 (AI 가속 vibe coding)

| 단계 | Epic·Story | 비고 |
|---|---|---|
| **Phase A** (Day 1) | EP-07 ST-07-1 (D-1 역산 + vc.confirmed 구독) | WorkingCalendar 재사용 — EP-06 패턴 그대로 |
| **Phase A** (Day 1) | EP-07 ST-07-2 (영업일 4주 회귀 + TC-EX-001) | EX_CONSTRAINT 풀 확장 trigger |
| **Phase B** (Day 1~2) | EP-08 ST-08-1 (4-shift × 75% effective_min) | 1 shift = 240 × 0.75 = 180 min |
| | EP-08 ST-08-2 (yield 수식 + BR-E05 reference) | `29673-2R060` = 2,531 검증 |
| | EP-08 ST-08-3 (Q_ext = max(0, Q_vc + target − current)) | 재고 통합 |
| **Phase C** (Day 2) | EP-09 ST-09-1 (셋팅 그룹 1~8 + shift 내 무 셋업) | 4주 회귀 셋업 0건 |
| **Phase D** (Day 2~3) | EP-EX11 ST-EX11-1 (검증 게이트 + p95 ≤ 2초) | TK-EX11-1-3 부하 측정 |
| | EP-EX12 ST-EX12-1 (충돌 대안 ≥ 3 distinct) | EP-VC15 패턴 재사용 |
| **Phase E** (Day 3) | EP-EX13 ST-EX13-1 (vc.changed 이벤트 구독 기반) | Sprint 4 완결 carry-over |
| **Phase F** (Day 3 종료) | Sprint 3 회고 + Sprint 4 진입 plan | |

병렬 가능 옵션:
- **A. EP-07 + EX_CONSTRAINT 풀 확장 병렬** — D-1 역산은 EP-06 패턴 재사용, EX 마스터 확장은 독립 작업 (다른 파일군)
- **B. EP-EX11 + EP-EX12 병렬** — 검증 + 대안은 다른 도메인 (게이트 vs 분류기)
- **C. EP-08 ST-08-1 + ST-08-2 + ST-08-3 순차 필수** — yield 수식 의존성 (effective_min → yield → Q_ext)

---

## 5. 신규 데이터베이스 마이그레이션 (예상 V017~V020)

| Migration | 테이블 / VIEW | Epic·Task |
|---|---|---|
| V017 | ALTER `master.ex_constraint` (압출 속도·길이·다이·라인 추가) | EP-07 / EP-08 |
| V018 | `app.ex_schedule` (압출 후보 + shift + due_date + yield) | EP-08 ST-08-3 |
| V019 | `master.ex_setting_group` (셋팅 번호 1~8 ↔ 호환 품번) | EP-09 ST-09-1 |
| V020 | `audit.ex_gate_result` (검증 게이트 pass/fail audit) | EP-EX11 |

> 모든 마이그레이션은 `backend/master/src/main/resources/db/migration/` 에 배치
> (Flyway 단일 classpath — 모듈 boundary 와 무관).

---

## 6. 신규 모듈 활성 (`com.scheduling.ex`)

`backend/ex/` 모듈은 Sprint 0~2 placeholder. Sprint 3 본격 구현:

```
com.scheduling.ex/
  shift/            ExShift enum (주간전반/후반/야간전반/후반) + EffectiveMin
  yield/            ExYieldCalculator (BR-E05) + YieldFormula
  required/         ExRequiredCalculator (Q_ext = max(0, Q_vc + target − current))
  deadline/         ExDeadlineCalculator (vc_date − 1 working day, BR-E01)
  grouping/         ShiftGroupingService (셋팅 그룹 1~8)
  domain/           ExSchedule + ExShiftSlot + SettingGroup
  gate/             ExGateValidator (누적 yield ≥ Q_ext)
  conflict/         ExConflictCategorizer + ExAlternativeGenerator + Report
  events/           VcConfirmedListener (vc.confirmed 구독)
```

### master 모듈 확장

```
com.scheduling.master.ex/
  ExSettingGroup + Repository + ExSettingGroupLookup facade
  + ALTER ex_constraint (속도·길이·다이·라인 풀 확장)
```

---

## 7. AllocationConflict 카테고리 확장 (EP-EX11/12)

압출 측 추가 카테고리 (vc.allocator.AllocationConflict 와 별도 — `com.scheduling.ex.gate.ExGateConflict.Category`):

- `EX_DEADLINE_D1` (BR-E01 D-1 위반)
- `EX_YIELD_SHORTAGE` (누적 yield < Q_ext)
- `EX_SHIFT_CAPA` (shift effective_min 초과)
- `EX_SETUP_REQUIRED` (shift 내 셋업 변경 발견 — BR-E06)
- `EX_GROUP_MIXED` (다른 셋팅 그룹 같은 shift)

압출 충돌 대안 (`com.scheduling.ex.conflict.ExAlternativeType`):

- `EARLIER_START` — 전일 야간 후반 활용
- `NIGHT_SECOND_BOOST` — NIGHT_SECOND shift 추가 배치
- `VC_DATE_NEGOTIATE` — vc_date +1 → ex_deadline +1 (성형 협상)
- `OUTSOURCE` — 외주 발주
- `SETUP_OVERRIDE` — shift 내 셋업 1회 허용 (예외)

---

## 8. Sprint 3 DoD (진입 게이트 충족 → 종료 게이트 목표)

| 영역 | 지표 | 목표 |
|---|---|---|
| **EP-07 D-1** | 영업일 회귀 (TC-EX-001 4주) | 위반 0 |
| **EP-08 BR-E05** | `29673-2R060` 주간전반 yield | = 2,531 (정확) |
| **EP-08 4-shift** | effective_min | 240 × 0.75 = 180 |
| **EP-09 셋업 0건** | 4주 회귀 (TC-EX-006) | shift 내 셋업 0 |
| **EP-EX11 p95** | 후보당 pass/fail | ≤ 2초 |
| **EP-EX12 대안** | ≥ 3 distinct | 100% |
| **EX_CONSTRAINT 풀 확장** | 47품번 속도·길이·다이·라인 | 100% seed |
| **Modulith verify** | 8 모듈 + ex.api facade | 0 위반 |
| **회귀** | DS-EX-* fixture 4주 시나리오 | 100% |

---

## 9. 차순위 carry-over (Sprint 4 이후)

| 항목 | 분류 | 이동 Sprint |
|---|---|---|
| EP-EX13 ST-EX13-1 (vc.changed 자동 재계산) | 자동 트리거 | Sprint 4 EP-10 완료 후 |
| EP-EX14 (변경 PUSH) | UI 통합 | Sprint 4 |
| EP-10 Confirmed 상태 | 거버넌스 | Sprint 4 EP-13 묶음 |
| EP-34 ST-34-1 (Dual-review) | 거버넌스 | Sprint 4 EP-13 묶음 |

---

## 10. 진입 게이트 체크리스트 (Sprint 2 완료 → Sprint 3 진입)

- [x] **Sprint 2 6 Epic 완료** (EP-04·05·06·21·VC15·VC16) — Sprint-2_Completion_v1.0 §11
- [x] **WorkingCalendar facade 안정화** (EP-06 + ProductSpec cross-master VIEW EP-21 ST-21-5)
- [x] **vc.confirmed 이벤트 발행 토대 존재** (VcScheduleStatus.CONFIRMED — Sprint 4 EP-10 에서 본격 활용)
- [x] **EX_CONSTRAINT 최소 도입** (V016 — Sprint 3 ST-07/08 에서 풀 확장)
- [x] **Modulith verify 0 위반** + ArchUnit 통과
- [x] **AI harness 안정** (40 commit · 46 PR-equivalent 작업 · 머지 충돌 0)

→ **Sprint 3 진입 승인 가능**. Phase A (EP-07 D-1 역산) 즉시 시작 가능.

---

## 11. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — Sprint 3 진입 계획 (EP-07·08·09·EX11·EX12 = ~22 SP, critical path) |
