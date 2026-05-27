# 작업 분할 구조서 (WBS) v1.13 — Sprint 17 EP-DAY-LOCK 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.13 | **작성일**: 2026-05-27
**전판**: [v1.12](TASK-001_WBS_v1.12.md) (Sprint 16 EP-CONFIRM 마감 Addendum)
**상태**: Addendum — Sprint 17 EP-DAY-LOCK (당일 락 + MES 폴백 BR-V07·X06) 100% 마감 + DoD 11/11 ✅ (MES stub baseline, 실 연동 Phase 5+ carry-over)

> v1.12 (Sprint 16 마감, 64 Epic / 330 SP 실) 의 §6 carry-over 중 **BR-V07 D-0 락 강화**, **MES 폴백 BR-X06**,
> **VcSchedule.createdBy actor 책임** 3건을 Sprint 17 가 마감. V043 + V044 migration + MES 패키지 신설.

---

## 1. v1.12 → v1.13 변경 요지

| 항목 | v1.12 (Sprint 16) | v1.13 (Sprint 17) |
|---|---|---|
| Epic 총수 | 64 | 65 (+ EP-DAY-LOCK) |
| SP 실 합 | 330 | **334** (+~4 실, 계획 4 정합) |
| Sprint 17 상태 | 계획 4 SP | ✅ **마감** (5 Story / 16 Task / 4 commits / ~0.7 PD AI 가속) |
| **BR-V07 D-0 락** | 일중 앵글 교체 만 (V027) | ✅ **V043 trg_vc_schedule_d0_lock** (production_date == today UPDATE 차단, override_reason 갱신 경로 예외) |
| **BR-X06 MES 폴백** | 정책만 (SRS REQ-FUNC-CO-004) | ✅ **V044 mes_shift_event + MesShiftPort + DegradedModeService** (stub baseline) |
| **createdBy actor** | 컬럼만 (V042) — legacy NULL fallback | ✅ **GreedyRotationAllocator 자동 set** (AllocationContext.requestedBy → VcSchedule.assignCreatedBy) |
| **Excel 폴백** | 미존재 | ✅ **POST /api/v1/mes/shift/fallback** (PLANNER+IT_OPS) + Frontend ExcelFallbackModal |
| **degraded mode UI** | 미존재 | ✅ **VcSimulationPage 상단 DegradedBanner** (30s polling + degraded 시 빨강 Alert) |
| **scheduling.mes.enabled** | (없음) | ✅ default `false` — Sprint 17 baseline 미연동 노이즈 차단 |
| **DoD** | 11/11 ✅ (carry-over 1건) | ✅ **11/11** (MES 실 연동 Phase 5+ carry-over) |

---

## 2. Sprint 17 마감 — EP-DAY-LOCK 5 Story 회고

### EP-DAY-LOCK 전체 (당일 락 + MES 폴백 BR-V07·X06)

**Sprint**: **S17** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-17_EP-DAY-LOCK_v1.0](PLAN-SPRINT-17_EP-DAY-LOCK_v1.0.md) (3-Day) / **SP 실**: ~4 / **선행**: EP-CONFIRM (S16)

| Story | 구현 |
|---|---|
| ST-DAY-LOCK-1 — BR-V07 D-0 락 강화 | V043 trg_vc_schedule_d0_lock (BEFORE UPDATE, OLD.production_date == CURRENT_DATE 차단, override_reason 갱신 경로 예외) + D0LockGuard (Clock 주입 KST, `isLocked()` + `enforce()`) + D0LockViolationException 423 매핑 + VcConfirmExceptionHandler 확장 |
| ST-DAY-LOCK-2 — createdBy actor 책임 완성 | AllocationContext record `requestedBy` 필드 추가 + backward-compat 4-arg 생성자 (기본 'system') + GreedyRotationAllocator `vc.assignCreatedBy(ctx.requestedBy())` 자동 호출. SwapProposalService/IntraDayOverrideService 는 UPDATE only (INSERT 없음, 추가 작업 0). OrderCommittedListener 는 Phase 5+ LOG only |
| ST-DAY-LOCK-3 — MES 인터페이스 + DegradedModeService | V044 app.mes_shift_event 테이블 (UNIQUE machine+date+shift, BR-X02 audit trigger) + MesShiftPort 인터페이스 + JpaMesShiftPort stub impl (UNIQUE 충돌 시 UPDATE) + DegradedModeService (1 shift = 6h 임계, mesEnabled flag default false) |
| ST-DAY-LOCK-4 — Excel 폴백 endpoint + Frontend UI | POST /api/v1/mes/shift/fallback (PLANNER+IT_OPS RBAC) + GET /api/v1/mes/degraded/status (4 role) + DegradedBanner (30s polling, mesEnabled=false 시 미렌더) + ExcelFallbackModal (Form 6 필드 + HttpError 분기) |
| ST-DAY-LOCK-5 — EP-DAY-LOCK IT + 회귀 | D0LockIT 7 cases + AllocatorCreatedByIT 2 cases + MesShiftAndDegradedIT 7 cases + Sprint 16 회귀 23 cases (ConfirmGateD2DualReview 13 + VcConfirmGate 5 + IntraDayLock 5) 모두 GREEN |

### Sprint 17 Task 매트릭스 (16 Task)

| Task | 소속 Story | SP 실 |
|---|---|---|
| TK-DAY-LOCK-1-1 V043 trg_vc_schedule_d0_lock | ST-DAY-LOCK-1 | 0.3 |
| TK-DAY-LOCK-1-2 D0LockGuard + Exception + 423 매핑 | ST-DAY-LOCK-1 | 0.3 |
| TK-DAY-LOCK-1-3 D0LockIT 7 cases | ST-DAY-LOCK-1 | 0.3 |
| TK-DAY-LOCK-2-1 AllocationContext.requestedBy + Allocator 호출 | ST-DAY-LOCK-2 | 0.3 |
| TK-DAY-LOCK-2-2 Listener actor (Phase 5+ LOG only) | ST-DAY-LOCK-2 | 0 (관찰) |
| TK-DAY-LOCK-2-3 Swap/Override INSERT 없음 (관찰) | ST-DAY-LOCK-2 | 0 |
| TK-DAY-LOCK-2-4 AllocatorCreatedByIT 2 cases | ST-DAY-LOCK-2 | 0.2 |
| TK-DAY-LOCK-3-1 MesShiftPort + JpaStub | ST-DAY-LOCK-3 | 0.3 |
| TK-DAY-LOCK-3-2 DegradedModeService (1 shift + snapshot) | ST-DAY-LOCK-3 | 0.3 |
| TK-DAY-LOCK-3-3 V044 mes_shift_event + audit trigger | ST-DAY-LOCK-3 | 0.3 |
| TK-DAY-LOCK-3-4 Slack/STOMP 알림 | (deferred Phase 5+) | 0 |
| TK-DAY-LOCK-4-1 MesController (Excel POST + status GET) | ST-DAY-LOCK-4 | 0.3 |
| TK-DAY-LOCK-4-2 DegradedBanner + ExcelFallbackModal | ST-DAY-LOCK-4 | 0.3 |
| TK-DAY-LOCK-5-1 MesShiftAndDegradedIT 7 cases | ST-DAY-LOCK-5 | 0.3 |
| TK-DAY-LOCK-5-2 Sprint 16 회귀 23 cases GREEN | ST-DAY-LOCK-5 | 0.1 |
| **Sprint 17 합계** | | **~4 SP** |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | D-0 row UPDATE 시도 → 423 + 한국어 (BR-V07) | ✅ V043 trigger + D0LockGuard + VcConfirmExceptionHandler |
| 2 | override_reason 비-NULL → D-0 UPDATE 통과 | ✅ D0LockIT.d0_override_path_allowed + guard_enforce_d0_override_allowed |
| 3 | 신규 VcSchedule.createdBy NOT NULL (Allocator) | ✅ AllocatorCreatedByIT 2 cases (explicit + legacy 'system') |
| 4 | MES 1 shift 미수신 → degraded mode 감지 | ✅ DegradedModeService.isDegraded + scheduling.mes.enabled flag |
| 5 | Excel 폴백 upload → source=EXCEL_FALLBACK + audit | ✅ POST /shift/fallback PLANNER 200 + V044 audit trigger |
| 6 | MES 정상 수신 시 degraded mode 자동 해제 | ✅ UNIQUE 충돌 시 UPDATE (lastReceivedShift 갱신 → 다음 polling 해제) |
| 7 | Frontend degraded mode 배너 + Excel upload Modal | ✅ DegradedBanner (30s polling) + ExcelFallbackModal (Form 6 필드) |
| 비기능 1 | ArchUnit GREEN (mes 패키지 boundary) | ✅ |
| 비기능 2 | Backend 신규 IT 16+ + 회귀 23 GREEN | ✅ All GREEN |
| 비기능 3 | TypeScript compile + frontend tests GREEN | ✅ tsc 0 errors + vitest 82/82 GREEN |
| 비기능 4 | V043/V044 migration 적용 | ✅ Testcontainers 부팅 정상 |

**기능 7 + 비기능 4 = 11/11 ✅**.

---

## 3. v1.12 §6 carry-over → v1.13 갱신

| 항목 | v1.12 carry-over | v1.13 결과 |
|---|---|---|
| **BR-V07 당일 락 trigger 통합 강화** | High Sprint 17 | ✅ **Sprint 17 ST-DAY-LOCK-1 마감** — V043 + D0LockGuard + 423 매핑 |
| **VcSchedule.createdBy actor 책임** | High Sprint 17 | ✅ **Sprint 17 ST-DAY-LOCK-2 마감** — Allocator 자동 set + legacy 'system' fallback |
| **BR-X06 MES 폴백** | High Sprint 17 | ✅ **Sprint 17 ST-DAY-LOCK-3·4 마감 (stub baseline)** — MesShiftPort + DegradedModeService + Excel POST + Frontend 배너/Modal. 실 MES 연동은 Phase 5+ carry-over |
| 본 PC 실 시나리오 E2E (Sprint 13~17 통합) + PLANNER 1/2 dual-review + D-0 락 시각 | High | ⏳ 잔여 (Sprint 19 베타 진입 직전 단일 시나리오) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Medium (변동 없음, Sprint 19 carry-over) |
| Order 자동 INSERT 흐름 | Medium | Phase 5+ (변동 없음) |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH (변동 없음) |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 (변동 없음) |
| **MES 실 연동 (HTTP/MQ/file adapter)** | (Sprint 17 신설) | High Phase 5+ — MesShiftPort 인터페이스 위에 adapter 교체. degraded mode 임계는 1 shift = 6h (조정 가능) |
| **TK-DAY-LOCK-3-4 Slack/STOMP degraded 알림** | (Sprint 17 deferred) | Medium Sprint 18 EP-NOTIFY — degraded 전이 시 push (REQ-FUNC-OC-009 정합) |

---

## 4. v1.2 § 추가 영향 정리 (v1.12 → v1.13 확장)

| § | v1.12 → v1.13 변경 |
|---|---|
| §9 Deferred Epic | + **EP-DAY-LOCK (S17 마감)** — BR-V07 D-0 락 강화 + BR-X06 MES 폴백 stub baseline + createdBy actor 완성 |
| §14 SP 합계 | 330 → **334** (Sprint 17 +~4 실, 계획 4 정합) |
| §16 Phase B 진입 조건 | + **Sprint 17 마감 → Sprint 18 EP-NOTIFY 진입 게이트 충족** (확정 정책 + 당일 락 + MES 폴백 baseline 완비) |
| §17 GitHub label | `sprint:S17` 추가 |
| §18 BR 추적 | BR-V07 강화 (D-0 락 trigger), BR-X06 신설 (MES 폴백 baseline), BR-X05 보강 (createdBy actor) |
| §19 Modulith 경계 | `com.scheduling.vc.mes` 패키지 신설 (vc 모듈 내부) — 실 MES 연동 시 별도 모듈 분리 검토 |

---

## 5. 신규 산출물 (Sprint 17)

### Backend Migration (vc 모듈)
- [V043__vc_schedule_d0_lock.sql](../../backend/vc/src/main/resources/db/migration/V043__vc_schedule_d0_lock.sql) — trg_vc_schedule_d0_lock
- [V044__create_mes_shift_event.sql](../../backend/vc/src/main/resources/db/migration/V044__create_mes_shift_event.sql) — mes_shift_event 테이블 + audit trigger

### Backend Service (vc 모듈)
- [D0LockGuard.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/D0LockGuard.java) — Clock 주입 KST, `isLocked()` + `enforce()`
- [D0LockViolationException.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/D0LockViolationException.java) — 423 매핑
- [VcConfirmExceptionHandler.java](../../backend/vc/src/main/java/com/scheduling/vc/confirm/VcConfirmExceptionHandler.java) — D0LockViolationException 423 + `brCode=BR-V07`
- [AllocationContext.java](../../backend/vc/src/main/java/com/scheduling/vc/allocator/AllocationContext.java) — `requestedBy` 필드 + backward-compat 4-arg 생성자
- [GreedyRotationAllocator.java](../../backend/vc/src/main/java/com/scheduling/vc/allocator/GreedyRotationAllocator.java) — `vc.assignCreatedBy(ctx.requestedBy())`
- [MesShiftSource.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftSource.java) — MES / EXCEL_FALLBACK enum
- [MesShiftEvent.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftEvent.java) — JPA entity
- [MesShiftEventRepository.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftEventRepository.java) — findTopByMachineIdOrderByReceivedAtDesc hot path
- [MesShiftPort.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftPort.java) — 인터페이스
- [JpaMesShiftPort.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/JpaMesShiftPort.java) — JPA stub impl + @Auditable
- [DegradedModeService.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/DegradedModeService.java) — 1 shift 임계 + snapshot
- [MesController.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesController.java) — POST /shift/fallback + GET /degraded/status

### Backend IT (app 모듈)
- [D0LockIT.java](../../backend/app/src/test/java/com/scheduling/integration/D0LockIT.java) — 7 cases (DB trigger 3 + service guard 4)
- [AllocatorCreatedByIT.java](../../backend/app/src/test/java/com/scheduling/integration/AllocatorCreatedByIT.java) — 2 cases
- [MesShiftAndDegradedIT.java](../../backend/app/src/test/java/com/scheduling/integration/MesShiftAndDegradedIT.java) — 7 cases

### Frontend (features/mes 신설)
- [mesApi.ts](../../frontend/src/features/mes/api/mesApi.ts) — fetchDegradedStatus + postShiftFallback
- [DegradedBanner.tsx](../../frontend/src/features/mes/components/DegradedBanner.tsx) — 30s polling 배너
- [ExcelFallbackModal.tsx](../../frontend/src/features/mes/components/ExcelFallbackModal.tsx) — Form 6 필드
- [VcSimulationPage.tsx](../../frontend/src/pages/VcSimulationPage.tsx) — 배너/Modal wiring

---

## 6. carry-over 식별 (Sprint 18+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 엑셀 E2E (Sprint 13~17 통합) + PLANNER 1/2 dual-review + D-0 락 + MES 폴백 시각 | High | Sprint 19 베타 진입 직전 단일 시나리오 검증 |
| **MES 실 연동 (HTTP/MQ/file adapter)** | High Phase 5+ | MesShiftPort 인터페이스 위에 adapter 교체. degraded mode 임계 1 shift = 6h (조정 가능) |
| **Slack/STOMP degraded 알림** | High Sprint 18 | EP-NOTIFY — degraded 전이 시 push (REQ-FUNC-OC-009 정합) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Sprint 19 carry-over |
| Order 자동 INSERT 흐름 (ImportOrchestrator → Allocator chain) | Medium | Phase 5+ allocator chain (Sprint 17 Allocator.requestedBy actor 완성으로 진입 게이트 충족) |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH cutover script |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.12](TASK-001_WBS_v1.12.md) — Sprint 16 마감
- [PLAN-SPRINT-17_EP-DAY-LOCK_v1.0](PLAN-SPRINT-17_EP-DAY-LOCK_v1.0.md) — Sprint 17 진입 plan (5 Story / 16 Task / DoD 11)
- [V043 migration](../../backend/vc/src/main/resources/db/migration/V043__vc_schedule_d0_lock.sql)
- [V044 migration](../../backend/vc/src/main/resources/db/migration/V044__create_mes_shift_event.sql)
- [MesShiftPort](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftPort.java)
- [DegradedBanner](../../frontend/src/features/mes/components/DegradedBanner.tsx)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.12 | 2026-05-27 | Claude Code | Sprint 9~16 마감 + V038 hotfix + AuditLogService hotfix + EX chain + EP-CONFIRM |
| 1.13 | 2026-05-27 | Claude Code | **Addendum — Sprint 17 EP-DAY-LOCK 100% 마감 (5 Story / 16 Task / ~4 SP). BR-V07 (D-0 락 trigger 강화) · BR-X06 (MES 폴백 stub baseline) · BR-X05 (createdBy actor 완성) 3건 동시 진행. V043 trigger + V044 mes_shift_event 테이블 + MesShiftPort/JpaStub + DegradedModeService (1 shift = 6h, scheduling.mes.enabled flag default false) + MesController (Excel POST + status GET) + DegradedBanner (30s polling) + ExcelFallbackModal. Backend IT 16 신규 + 23 회귀 모두 GREEN, Frontend tsc 0 + vitest 82/82 GREEN. DoD 11/11 ✅ (MES 실 연동은 Phase 5+ carry-over — adapter 교체 패턴). 65 Epic / 334 SP 실. Sprint 18 EP-NOTIFY 진입 게이트 충족 — 확정 정책 + 당일 락 + MES 폴백 baseline 완비. 베타 진입도 8/10 (S10~17 완료)** |
