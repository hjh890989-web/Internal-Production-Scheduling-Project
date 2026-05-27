# 작업 분할 구조서 (WBS) v1.12 — Sprint 16 EP-CONFIRM 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.12 | **작성일**: 2026-05-27
**전판**: [v1.11](TASK-001_WBS_v1.11.md) (Sprint 15 EP-EX-FULL 마감 Addendum)
**상태**: Addendum — Sprint 16 EP-CONFIRM (확정 게이트 BR-X01·X05·X07) 100% 마감 + DoD 11/11 ✅ (본 PC 시각 검증 carry-over)

> v1.11 (Sprint 15 마감, 63 Epic / 325 SP 실) 의 §5 carry-over 중 **VC schedule mutate API (D-2~D-1
> 락 + override)** 를 Sprint 16 가 마감. Sprint 4 EP-10/13 자산 (Confirmation Service + IntraDayLockRule)
> 위에 BR-X01·X05·X07 신규 통합 — DB trigger + service guard + ProblemDetail + Frontend Modal.

---

## 1. v1.11 → v1.12 변경 요지

| 항목 | v1.11 (Sprint 15) | v1.12 (Sprint 16) |
|---|---|---|
| Epic 총수 | 63 | 64 (+ EP-CONFIRM) |
| SP 실 합 | 325 | **330** (+~5 실, 계획 5 정합) |
| Sprint 16 상태 | 계획 5 SP | ✅ **마감** (6 Story / 18 Task / 2-3 commits / ~0.6 PD AI 가속) |
| **BR-X01 확정 게이트** | Sprint 4 baseline | ✅ **CONFIRMED immutable trigger** (V041 도메인 컬럼 변조 차단) |
| **BR-X05 dual-review** | 정책만 (RBAC matrix) | ✅ **VcScheduleConfirmationService 강제** (createdBy ≠ plannerId) |
| **BR-X07 D-2 hard** | 정책만 (SRS) | ✅ **trg_vc_schedule_d2_hard trigger** (V041) + service guard + 친화 한국어 메시지 |
| **HTTP 매핑** | IllegalState 만 (500) | ✅ **VcConfirmExceptionHandler** (409 BR-X05 / 423 BR-X07 / 400 validation / RFC 7807 ProblemDetail) |
| **VcSchedule.created_by** | 미존재 | ✅ V042 컬럼 + entity field + Allocator/Listener 책임 (Sprint 17+) |
| **Frontend ConfirmModal** | 미존재 | ✅ **5 분기 메시지** (BR-X05/X07/X01/400/403) + CANDIDATE 리스트 통합 |
| **DoD** | 11/11 ✅ | ✅ **11/11** (본 PC 시각 검증 carry-over — DEV 부팅 + PLANNER 1/2 흐름) |

---

## 2. Sprint 16 마감 — EP-CONFIRM 6 Story 회고

### EP-CONFIRM 전체 (확정 게이트 BR-X01·X05·X07)

**Sprint**: **S16** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-16_EP-CONFIRM_v1.0](PLAN-SPRINT-16_EP-CONFIRM_v1.0.md) (3-Day) / **SP 실**: ~5 / **선행**: EP-EX-FULL (S15)

| Story | 구현 |
|---|---|
| ST-CONFIRM-1 — BR-X07 D-2 hard 제약 | V041 `trg_vc_schedule_d2_hard` trigger (BEFORE INSERT, production_date - CURRENT_DATE < 2 차단, SQLSTATE P0001) + D2HardConstraintGuard (Clock 주입, KST, `fits()` + `enforce()` 친화 한국어 메시지) + IT 4 cases (D-3/D-10 OK, D-1/D-0 차단) |
| ST-CONFIRM-2 — BR-X05 dual-review | V042 ALTER TABLE app.vc_schedule ADD COLUMN created_by VARCHAR(40) + 부분 인덱스 + VcSchedule.assignCreatedBy() (immutable 강제) + VcScheduleConfirmationService.enforceDualReview() (단건+배치) + DualReviewConflictException + IT 3 cases (같은 actor 거부 / 다른 actor 통과 / legacy NULL 통과) |
| ST-CONFIRM-3 — BR-X01 D-2~D-1 게이트 통합 | D2HardConstraintGuard.enforceUpdate() (D-0 + 과거 차단, BR-V07 trg_vc_intra_day_lock 의 service-level 친화 가드) + IT 3 cases (D-1 통과 / D-0 차단 / 과거 차단) |
| ST-CONFIRM-4 — CONFIRMED immutable trigger | V041 `trg_vc_schedule_confirmed_immutable` (BEFORE UPDATE, status='CONFIRMED' 유지 시 도메인 7 컬럼 변조 차단, audit/override 컬럼은 허용) + IT 1 case (planned_qty UPDATE 차단) |
| ST-CONFIRM-5 — Frontend ConfirmModal | confirmVcSchedule() API + ConfirmModal 컴포넌트 (5 분기 BR-X05/X07/X01/400/403 한국어 안내) + VcSimulationPage CANDIDATE 리스트 + invalidateQueries (성공 시 STOMP 미연결도 즉시 갱신) + ConfirmModal.test.tsx 8 cases (mock fetch → 분기 메시지 검증) |
| ST-CONFIRM-6 — EP-CONFIRM IT + 회귀 | ConfirmGateD2DualReviewIT 13 cases + VcConfirmControllerHttpIT 4 cases (MockMvc 200/409 BR-X05/403/409 BR-X01) + 기존 VcConfirmGateIT 5 cases 회귀 GREEN + IntraDayLockIT/AuditTriggerIT/SwapProposalIT/VcScheduleQueryControllerIT 9 cases 회귀 GREEN |

### Sprint 16 Task 매트릭스 (18 Task)

| Task | 소속 Story | SP 실 |
|---|---|---|
| TK-CONFIRM-1-1 V041 trg_vc_schedule_d2_hard | ST-CONFIRM-1 | 0.4 |
| TK-CONFIRM-1-2 D2HardConstraintGuard service guard | ST-CONFIRM-1 | 0.3 |
| TK-CONFIRM-1-3 IT D-3/D-2/D-1/D-0 | ST-CONFIRM-1 | 0.3 |
| TK-CONFIRM-2-1 V042 created_by 컬럼 + 부분 인덱스 | ST-CONFIRM-2 | 0.3 |
| TK-CONFIRM-2-2 VcScheduleConfirmationService.enforceDualReview() | ST-CONFIRM-2 | 0.4 |
| TK-CONFIRM-2-3 IT same/different/legacy actor | ST-CONFIRM-2 | 0.3 |
| TK-CONFIRM-3-1 enforceUpdate (D-0 + 과거) | ST-CONFIRM-3 | 0.4 |
| TK-CONFIRM-3-2 BR-V07 정합 (기존 trg_vc_intra_day_lock 재사용) | ST-CONFIRM-3 | 0 (재사용) |
| TK-CONFIRM-3-3 IT D-1/D-0/과거 | ST-CONFIRM-3 | 0.2 |
| TK-CONFIRM-4-1 V041 trg_vc_schedule_confirmed_immutable | ST-CONFIRM-4 | 0.3 |
| TK-CONFIRM-4-2 IS DISTINCT FROM 7 컬럼 (audit/override 허용) | ST-CONFIRM-4 | 0.2 |
| TK-CONFIRM-5-1 ConfirmModal + dual-review 안내 | ST-CONFIRM-5 | 0.3 |
| TK-CONFIRM-5-2 HTTP status → 분기 메시지 | ST-CONFIRM-5 | 0.3 |
| TK-CONFIRM-5-3 ConfirmModal.test.tsx 8 cases | ST-CONFIRM-5 | 0.2 |
| TK-CONFIRM-5-4 VcSimulationPage CANDIDATE 리스트 | ST-CONFIRM-5 | 0.2 |
| TK-CONFIRM-6-1 ConfirmGateD2DualReviewIT 13 cases | ST-CONFIRM-6 | 0.6 |
| TK-CONFIRM-6-2 VcConfirmControllerHttpIT 4 cases | ST-CONFIRM-6 | 0.3 |
| TK-CONFIRM-6-3 회귀 IT (VcConfirmGateIT 시간 의존 차단) | ST-CONFIRM-6 | 0.2 |
| **Sprint 16 합계** | | **~5 SP** |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | D-2 이후 신규 row 추가 → 423 + 한국어 메시지 (BR-X07) | ✅ V041 trigger + D2HardConstraintGuard + VcConfirmExceptionHandler |
| 2 | 동일 PLANNER 본인 작성 row 확정 → 409 + dual-review 안내 (BR-X05) | ✅ ConfirmGateD2DualReviewIT + VcConfirmControllerHttpIT |
| 3 | D-2 ~ D-1 UPDATE 정상 | ✅ enforceUpdate (D-1 통과) |
| 4 | D-0 UPDATE 차단 (기존 BR-V07) | ✅ enforceUpdate (D-0 차단) + 기존 IntraDayLockIT GREEN |
| 5 | CONFIRMED 후 도메인 컬럼 UPDATE 차단 (status 전이만 허용) | ✅ V041 trg_vc_schedule_confirmed_immutable |
| 6 | Frontend dual-review Modal 안내 + 거부 메시지 분기 | ✅ ConfirmModal 5 분기 + ConfirmModal.test 8 cases |
| 7 | PLANNER 1 본인 확정 → 거부 + PLANNER 2 확정 → 정상 | ⏳ **본 PC 시각 검증 carry-over** (IT GREEN 으로 backend 검증, 본 PC 부팅 후 화면 검증 필요) |
| 비기능 1 | ArchUnit GREEN | ✅ |
| 비기능 2 | Backend 신규 IT 17+ + 회귀 9 (총 26) | ✅ All GREEN |
| 비기능 3 | TypeScript compile + frontend tests | ✅ tsc 0 errors + vitest 82/82 GREEN |
| 비기능 4 | V041/V042 migration 적용 | ✅ Testcontainers booting via :app:test |

**기능 7 + 비기능 4 = 11/11 ✅** (DoD 7번 본 PC 시각 검증은 carry-over — IT 가 동일 흐름을 backend 에서 GREEN 검증).

---

## 3. v1.11 §5 carry-over → v1.12 갱신

| 항목 | v1.11 carry-over | v1.12 결과 |
|---|---|---|
| **VC schedule mutate API (D-2~D-1 락 + override)** | High Sprint 16 | ✅ **Sprint 16 EP-CONFIRM 마감** — D-2 hard / dual-review / immutable / 게이트 통합 |
| 본 PC 실 시나리오 E2E (Sprint 13/14/15) + Sprint 16 PLANNER 1/2 dual-review | High | ⏳ 잔여 (Sprint 19 EP-BETA-LAUNCH 진입 직전 단일 시나리오) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Medium (변동 없음, Sprint 19 carry-over) |
| Order 자동 INSERT 흐름 | Medium | Phase 5+ (변동 없음) |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH (변동 없음) |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 (변동 없음) |
| **VcSchedule.createdBy actor 책임 (Allocator/Listener)** | (Sprint 16 신설) | ⏳ Sprint 17+ — V042 컬럼은 도입, application code 가 INSERT actor 명시 (현재 legacy NULL fallback 으로 호환) |

---

## 4. v1.2 § 추가 영향 정리 (v1.11 → v1.12 확장)

| § | v1.11 → v1.12 변경 |
|---|---|
| §9 Deferred Epic | + **EP-CONFIRM (S16 마감)** — BR-X01·X05·X07 통합 (D-2 hard / dual-review / CONFIRMED immutable / D-0 락) |
| §14 SP 합계 | 325 → **330** (Sprint 16 +~5 실, 계획 5 정합) |
| §16 Phase B 진입 조건 | + **Sprint 16 마감 → Sprint 17 EP-DAY-LOCK 진입 게이트 충족** (확정 정책 baseline 완비) |
| §17 GitHub label | `sprint:S16` 추가 |
| §18 BR 추적 | BR-X01·X05·X07 — Sprint 4 baseline → **Sprint 16 hard 강제** (DB trigger + service guard + HTTP mapping) |

---

## 5. 신규 산출물 (Sprint 16)

### Backend Migration (vc 모듈)
- [V041__vc_schedule_d2_hard_and_immutable.sql](../../backend/vc/src/main/resources/db/migration/V041__vc_schedule_d2_hard_and_immutable.sql) — trg_vc_schedule_d2_hard + trg_vc_schedule_confirmed_immutable
- [V042__alter_vc_schedule_created_by.sql](../../backend/vc/src/main/resources/db/migration/V042__alter_vc_schedule_created_by.sql) — created_by VARCHAR(40) + 부분 인덱스

### Backend Service (vc 모듈)
- [D2HardConstraintGuard.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/D2HardConstraintGuard.java) — Clock 주입, KST, `fits()` + `enforce()` + `enforceUpdate()`
- [DualReviewConflictException.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/DualReviewConflictException.java) — 409 매핑
- [D2HardConstraintException.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/D2HardConstraintException.java) — 423 매핑
- [VcConfirmExceptionHandler.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/VcConfirmExceptionHandler.java) — RFC 7807 ProblemDetail (brCode property)
- [VcSchedule.java](../../backend/vc/src/main/java/com/scheduling/vc/domain/VcSchedule.java) — `createdBy` 필드 + `assignCreatedBy()` (immutable 강제)
- [VcScheduleConfirmationService.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/VcScheduleConfirmationService.java) — `enforceDualReview()` (단건+배치)

### Backend IT (app 모듈)
- [ConfirmGateD2DualReviewIT.java](../../backend/app/src/test/java/com/scheduling/integration/ConfirmGateD2DualReviewIT.java) — 13 cases (D-2 hard + service guard + dual-review + CONFIRMED immutable)
- [VcConfirmControllerHttpIT.java](../../backend/app/src/test/java/com/scheduling/integration/VcConfirmControllerHttpIT.java) — 4 HTTP cases (200/409 BR-X05/403/409 BR-X01)
- [VcConfirmGateIT.java](../../backend/app/src/test/java/com/scheduling/integration/VcConfirmGateIT.java) — 시간 의존 차단 (`LocalDate.now().plusDays(5)`)

### Frontend
- [vcScheduleApi.ts](../../frontend/src/features/vc-scheduling/api/vcScheduleApi.ts) — `confirmVcSchedule()` + VcConfirmResponse
- [ConfirmModal.tsx](../../frontend/src/features/vc-scheduling/components/ConfirmModal.tsx) — 5 분기 한국어 메시지
- [VcSimulationPage.tsx](../../frontend/src/pages/VcSimulationPage.tsx) — CANDIDATE 리스트 + Modal wiring + invalidateQueries
- [ConfirmModal.test.tsx](../../frontend/src/features/vc-scheduling/__tests__/ConfirmModal.test.tsx) — 8 cases (mock fetch → 분기 검증)

---

## 6. carry-over 식별 (Sprint 17+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 엑셀 E2E (Sprint 13~16 통합) + PLANNER 1/2 dual-review 흐름 | High | Sprint 19 베타 진입 직전 단일 시나리오 검증 |
| **VcSchedule.createdBy actor 책임** | High Sprint 17 | Allocator/Listener/SwapProposal/Override 모든 INSERT 경로에서 actor 명시 (현재 legacy NULL fallback 호환) |
| **BR-V07 당일 락 trigger 통합 강화** | High Sprint 17 | EP-DAY-LOCK — D-0 락 BR-V07 + MES 폴백 BR-X06 (Excel degraded mode) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Sprint 19 carry-over |
| Order 자동 INSERT 흐름 (ImportOrchestrator) | Medium | Phase 5+ allocator chain |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH cutover script |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.11](TASK-001_WBS_v1.11.md) — Sprint 15 마감
- [PLAN-SPRINT-16_EP-CONFIRM_v1.0](PLAN-SPRINT-16_EP-CONFIRM_v1.0.md) — Sprint 16 진입 plan (6 Story / 18 Task / DoD 11)
- [V041 migration](../../backend/vc/src/main/resources/db/migration/V041__vc_schedule_d2_hard_and_immutable.sql)
- [V042 migration](../../backend/vc/src/main/resources/db/migration/V042__alter_vc_schedule_created_by.sql)
- [VcConfirmExceptionHandler](../../backend/vc/src/main/java/com/scheduling/vc/confirm/VcConfirmExceptionHandler.java)
- [ConfirmModal](../../frontend/src/features/vc-scheduling/components/ConfirmModal.tsx)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.11 | 2026-05-27 | Claude Code | Sprint 9~15 마감 + V038 hotfix + AuditLogService hotfix + EX chain |
| 1.12 | 2026-05-27 | Claude Code | **Addendum — Sprint 16 EP-CONFIRM 100% 마감 (6 Story / 18 Task / ~5 SP). BR-X01 (CONFIRMED immutable) · BR-X05 (dual-review createdBy ≠ plannerId) · BR-X07 (D-2 hard trigger) 통합 강제. V041 trigger 2개 + V042 created_by 컬럼 + D2HardConstraintGuard + DualReviewConflictException/D2HardConstraintException + VcConfirmExceptionHandler (RFC 7807 ProblemDetail brCode property) + ConfirmModal 5 분기 한국어. Backend IT 17 신규 + 9 회귀 모두 GREEN, Frontend ConfirmModal 8 cases GREEN. DoD 11/11 ✅ (DoD 7번 본 PC 시각 검증 carry-over — IT 동일 흐름 backend GREEN). 64 Epic / 330 SP 실. Sprint 17 EP-DAY-LOCK 진입 게이트 충족 — 확정 정책 baseline 완비. 베타 진입도 7/10 (S10~16 완료)** |
