# 작업 분할 구조서 (WBS) v1.11 — Sprint 15 EP-EX-FULL 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.11 | **작성일**: 2026-05-27
**전판**: [v1.10](TASK-001_WBS_v1.10.md) (Sprint 14 EP-VC-FULL 마감 Addendum)
**상태**: Addendum — Sprint 15 EP-EX-FULL (압출 PDD-03 완성) 100% 마감 + DoD 11/11 ✅ (carry-over 없음)

> v1.10 (Sprint 14 마감, 63 Epic / 321 SP 실) 의 §6 carry-over 식별된 항목 중 **압출 chain
> 검증** 을 Sprint 15 가 마감. Sprint 1~6 누적 자산 매우 풍부 — 신규 작업 최소화.

---

## 1. v1.10 → v1.11 변경 요지

| 항목 | v1.10 (Sprint 14) | v1.11 (Sprint 15) |
|---|---|---|
| Epic 총수 | 63 | 63 (변동 없음, EP-EX-FULL 마감만) |
| SP 실 합 | 321 | **325** (+~4 실, Sprint 15 계획 5 대비 -1 — 누적 자산 활용) |
| Sprint 15 상태 | 계획 5 SP | ✅ **마감** (6 Story / 14 Task / 2 commits / ~0.5 PD AI 가속) |
| VC → EX chain | Sprint 6 EP-EX13/14 (이미 완성) | ✅ **chain IT 명시 검증** (VcToExChainIT 2 cases) |
| EX 매트릭스 sample | 빈 grid | ✅ **V040 seed** — V039 vc_schedule sub-select chain (15 row) |
| BR-E01 (D-1 역산) | 기존 IT | ✅ DeadlineRegression IT GREEN 확인 |
| BR-E05 (수율 2531) | 기존 IT | ✅ YieldAndDemand IT GREEN ("29673-2R060 = 2,531" 정확) |
| ExMatrixPage UX | STOMP Tag 만 | ✅ + **[← 성형 시뮬뷰] link** + 빈 grid Alert info |
| **DoD** | 10/11 + 1 carry-over | ✅ **11/11 (carry-over 없음)** |

---

## 2. Sprint 15 마감 — EP-EX-FULL 6 Story 회고

### EP-EX-FULL 전체 (압출 PDD-03 완성)

**Sprint**: **S15** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-15_EP-EX-FULL_v1.0](PLAN-SPRINT-15_EP-EX-FULL_v1.0.md) (2-Day) / **SP 실**: ~4 / **선행**: EP-VC-FULL (S14)

| Story | 구현 | Commit |
|---|---|---|
| ST-EX-1 — VcConfirmedListener + VcChangedListener 검증 IT | VcToExChainIT 2 cases — VcConfirmed → ExtrusionScheduleService.generateCandidates (EP-07 chain) + VcChanged → PartialReplanService.replanWithContext (EP-EX13 BR-X03 chain). TransactionTemplate AFTER_COMMIT trigger 패턴 (Sprint 14 재사용) + @MockitoBean 격리 | `caa629d` |
| ST-EX-2 — V040 ex_schedule_candidate sample seed | V039 sample vc_schedule (99999-SAMPLE-*) 의 vc_schedule_id 를 sub-select 로 동적 매핑 + BR-E01 자동 (`extrusion_deadline = vc_production_date - 1 day`) + WHERE NOT EXISTS idempotent — 15 row 자동 chain 생성 | `caa629d` |
| ST-EX-3 — BR-E01 / BR-E05 회귀 IT (기존 자산 검증) | DeadlineRegression IT + YieldAndDemand IT 기존 GREEN 확인 — Sprint 5/6 누적, 추가 작업 0 (BR-E05 reference "29673-2R060 주간전반 = 2,531" 정확) | (기존) |
| ST-EX-4 — ExMatrixPage UX (Sprint 14 패턴 정합) | 상단 우측 [← 성형 시뮬뷰] navigate 버튼 + RangePicker/Excel 다운로드 left group / STOMP Tag + cascade right group 정리 + 빈 grid Alert info ("V040 시드 99999-SAMPLE-EX-* 가 있어야 표시") | `148bd30` |
| ST-EX-5 — 다중 후보 ranking (EP-18) 시각 검증 | CandidateRankingTable + ranking Tab (이미 완성) — 추가 작업 0 | (기존) |
| ST-EX-6 — EX-FULL 회귀 IT + DoD | 신규 VcToExChainIT + 기존 BR IT 모두 GREEN + ArchUnit (Modulith 9 모듈) | `caa629d` |

### Sprint 15 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-EX-1-1 VcConfirmedToEx chain IT | ST-EX-1 | 0.3 | `caa629d` |
| TK-EX-1-2 VcChangedToExPartialReplan IT | ST-EX-1 | 0.2 | `caa629d` |
| TK-EX-2-1 V040 sample seed (sub-select chain) | ST-EX-2 | 0.6 | `caa629d` |
| TK-EX-2-2 WHERE NOT EXISTS pattern | ST-EX-2 | 0 (재사용) | — |
| TK-EX-2-3 FK 충족 (vc_schedule 이미 V039) | ST-EX-2 | 0.1 | `caa629d` |
| TK-EX-3-1~3 BR-E01/E05 기존 IT 확인 (추가 0) | ST-EX-3 | 0 | (기존) |
| TK-EX-4-1 ExMatrixPage 안내 Alert | ST-EX-4 | 0.3 | `148bd30` |
| TK-EX-4-2 [← 성형 시뮬뷰] navigate 버튼 | ST-EX-4 | 0.2 | `148bd30` |
| TK-EX-5-1 Ranking 시각 검증 (이미 완성) | ST-EX-5 | 0 | (기존) |
| TK-EX-6-1~2 회귀 + ArchUnit | ST-EX-6 | 0.2 | `148bd30` |
| **Sprint 15 합계** | | **~4 SP** | (계획 5 -1 / AI 가속 ~0.5 PD 실 — 계획 2.2 PD 의 23%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | VcConfirmed → ExtrusionScheduleService chain | ✅ VcToExChainIT |
| 2 | VcChanged → PartialReplanService chain (BR-X03) | ✅ VcToExChainIT |
| 3 | 압출 매트릭스 sample (V040 + V039 chain 15 row) | ✅ |
| 4 | BR-E01 D-1 역산 검증 | ✅ 기존 IT GREEN |
| 5 | BR-E05 수율 2531 reference | ✅ 기존 IT GREEN ("29673-2R060 = 2,531") |
| 6 | Excel 다운로드 (EP-12) | ✅ 기존 |
| 7 | 다중 후보 ranking (EP-18) | ✅ 기존 |
| 8 | STOMP Tag + 빈 grid 안내 + 시뮬뷰 link | ✅ ST-EX-4 |
| 비기능 1~3 | ArchUnit + IT + tsc | ✅ All GREEN |

**기능 8 + 비기능 3 = 11/11 ✅ (carry-over 없음).**

---

## 3. v1.10 §6 carry-over → v1.11 갱신

| 항목 | v1.10 carry-over | v1.11 결과 |
|---|---|---|
| 본 PC 실 시나리오 E2E (Sprint 13/14) | High | ⏳ 여전히 잔여 (Sprint 16 진입 직전) |
| VC schedule mutate API | High Sprint 15~16 | (Sprint 16 EP-CONFIRM 부속 — D-2~D-1 락 + override) |
| 장비/셋팅/합금형/라인 CRUD UI | Medium | Medium (변동 없음, Sprint 19 carry-over) |
| Order 자동 INSERT 흐름 | Medium | Phase 5+ (변동 없음) |
| **EX chain 시각 검증** | (식별 안 됨) | ✅ **Sprint 15 chain IT + V040 seed 마감** |
| **EX UX 일원화** | (식별 안 됨) | ✅ **Sprint 15 ST-EX-4 [← 성형 시뮬뷰] link 통합** |

---

## 4. v1.2 § 추가 영향 정리 (v1.10 → v1.11 확장)

| § | v1.10 → v1.11 변경 |
|---|---|
| §9 Deferred Epic | + **EP-EX-FULL (S15 마감)** — VC → EX chain 명시 검증 + V040 sample + UX 일원화 |
| §14 SP 합계 | 321 → **325** (Sprint 15 +~4 실) |
| §16 Phase B 진입 조건 | + **Sprint 15 마감 → Sprint 16 EP-CONFIRM 진입 게이트 충족** (VC + EX schedule SSoT 흐름 완비) |
| §17 GitHub label | `sprint:S15` 추가 |

---

## 5. carry-over 식별 (Sprint 16+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 엑셀 E2E (Sprint 13/14/15 통합) | High | Sprint 19 베타 진입 직전 단일 시나리오 검증 |
| VC schedule mutate API (PATCH /slots/{id}, override) | High | **Sprint 16 EP-CONFIRM 부속** (D-2~D-1 락 + override audit) |
| 장비 (LP/IC) / 셋팅 그룹 / 합금형 / 회전수 / 라인 5 entity CRUD UI | Medium | Sprint 19 carry-over |
| Order 자동 INSERT 흐름 (ImportOrchestrator → diff → commit chain) | Medium | Phase 5+ allocator chain |
| 99999-SAMPLE-* (Sprint 14 V039) + 99999-SAMPLE-EX-* (Sprint 15 V040) PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH cutover script |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.10](TASK-001_WBS_v1.10.md) — Sprint 13 hotfix + Sprint 14 마감
- [PLAN-SPRINT-15_EP-EX-FULL_v1.0](PLAN-SPRINT-15_EP-EX-FULL_v1.0.md) — Sprint 15 진입 plan (6 Story / 14 Task / DoD 11)
- [Sprint 15 commits chain](#) — `caa629d` (Day 1 Chain IT + V040 seed) → `148bd30` (Day 2 ExMatrixPage UX)
- [VcToExChainIT](../../backend/app/src/test/java/com/scheduling/integration/VcToExChainIT.java)
- [V040 seed](../../backend/ex/src/main/resources/db/migration/V040__seed_ex_candidate_sample.sql)

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.10 | 2026-05-27 | Claude Code | Sprint 9 plan + Sprint 10·11·12·13·14 마감 + V038 hotfix + AuditLogService hotfix |
| 1.11 | 2026-05-27 | Claude Code | **Addendum — Sprint 15 EP-EX-FULL 100% 마감 (6 Story / 14 Task / 2 commits / ~4 SP). VC → EX chain (Sprint 6 EP-EX13/14) 명시 IT + V040 sample seed (V039 sub-select chain) + ExMatrixPage UX 일원화 ([← 성형 시뮬뷰] link). BR-E01/E05 기존 IT 모두 GREEN 확인 — 추가 작업 0. DoD 11/11 ✅ (carry-over 없음, 1번째 100% sprint). 63 Epic / 325 SP 실. Sprint 16 EP-CONFIRM 진입 게이트 충족 — VC + EX schedule SSoT 흐름 완비. 베타 진입도 6/10 (S10~15 완료)** |
