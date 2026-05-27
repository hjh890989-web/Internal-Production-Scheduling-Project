# Sprint 17 진입 계획 — EP-DAY-LOCK (당일 락 + MES 폴백 BR-V07·X06) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 17 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 17 baseline](TASK-001_WBS_v1.5.md) + [WBS v1.12 §6 carry-over](TASK-001_WBS_v1.12.md) + REF-SRS BR-V07/X06 (REQ-FUNC-CO-004 / REQ-NF-REL-004) + [PLAN-SPRINT-16_EP-CONFIRM_v1.0](PLAN-SPRINT-16_EP-CONFIRM_v1.0.md)

---

## 1. 목적

**Sprint 16 EP-CONFIRM 직후 진입** — 확정 게이트 (D-2 hard + dual-review + CONFIRMED immutable) 완비된 상태에서 **D-0 (당일) 락 강화** + **MES 폴백 (degraded mode) 신설**:

| BR | 정책 | 적용 단계 |
|---|---|---|
| **BR-V07 (강화)** | D-0 락 — production_date == today row UPDATE 차단 (override_reason 경로만 예외) | DB trigger `trg_vc_schedule_d0_lock` + Service guard |
| **BR-X06 (신설)** | MES 연동 실패 → Excel 폴백 (1 shift 미수신 시 degraded mode) | MES 인터페이스 + DegradedModeService + Excel upload endpoint |
| **BR-X05 (보강)** | VcSchedule.createdBy actor 책임 완성 — 모든 INSERT 경로 명시 | Allocator/Listener/SwapProposal/Override |
| BR-V07 (기존) | 일중 앵글 교체 차단 (override_reason 강제) | V027 trg_vc_intra_day_lock (유지) |
| BR-X01 (기존) | CONFIRMED 도메인 컬럼 immutable | V041 trg_vc_schedule_confirmed_immutable (유지) |

**현황 인벤토리:**
- ✅ Sprint 4 EP-13 — BR-V07 일중 락 (IntraDayLockRule + trg_vc_intra_day_lock)
- ✅ Sprint 16 EP-CONFIRM — BR-X07 D-2 hard + BR-X05 dual-review + CONFIRMED immutable
- ⏳ **CANDIDATE row 의 D-0 UPDATE** — 현재 차단 안 됨 (BR-V07 강화 필요)
- ⏳ **MES 연동** — 정책만 SRS 기재 (REQ-FUNC-CO-004 / REQ-NF-REL-004), 코드 0건
- ⏳ **VcSchedule.createdBy actor 책임** — V042 컬럼만 도입, application code 미작성 (legacy NULL fallback)

**활성 후 효과:**
- D-0 row UPDATE 시도 → 거절 (BR-V07 hard, override_reason 경로만 예외)
- MES 실적 1 shift 미수신 → degraded mode 전환 + Slack 알림 + Excel 수동 입력 가능
- 모든 신규 VcSchedule row 에 createdBy 자동 set (Sprint 16 dual-review 정합)
- Sprint 18 EP-NOTIFY 진입 게이트 — 운영 알림 baseline 완비

---

## 2. Sprint 17 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-DAY-LOCK-1 BR-V07 D-0 락 강화 (V043 trigger + service guard) | 0.8 | 0.4 |
| ST-DAY-LOCK-2 VcSchedule.createdBy actor 책임 완성 (Allocator/Listener/Override) | 1.0 | 0.5 |
| ST-DAY-LOCK-3 MES 인터페이스 + DegradedModeService (BR-X06) | 1.2 | 0.6 |
| ST-DAY-LOCK-4 Excel 폴백 endpoint + Frontend UI 알림 | 0.6 | 0.3 |
| ST-DAY-LOCK-5 EP-DAY-LOCK IT 5+ cases + 회귀 | 0.4 | 0.2 |
| **합계** | **~4 SP** | **~2 PD** |

> **WBS v1.5 계획 4 SP 정합.**

---

## 3. 의존성 DAG

```
ST-DAY-LOCK-1 (BR-V07 D-0 trigger)
    ↓
ST-DAY-LOCK-2 (createdBy actor) ──┐
                                  │
ST-DAY-LOCK-3 (MES degraded)      │
                                  ↓
ST-DAY-LOCK-4 (Excel fallback UI)
                                  ↓
                          ST-DAY-LOCK-5 (IT + DoD)
```

**병렬 윈도우:**
- **ST-DAY-LOCK-1 ↔ ST-DAY-LOCK-2** — DB trigger vs application code 분리
- **ST-DAY-LOCK-3 ↔ ST-DAY-LOCK-4** — backend interface vs frontend UI 분리

---

## 4. Story · Task 매트릭스

### ST-DAY-LOCK-1 — BR-V07 D-0 락 강화

| Task | 내용 | SP |
|---|---|:--:|
| TK-DAY-LOCK-1-1 | V043 `trg_vc_schedule_d0_lock` trigger — BEFORE UPDATE, OLD.production_date == CURRENT_DATE 이면 차단 (override_reason 비-NULL 경로만 예외, BR-V07 정합). RAISE SQLSTATE 'P0001' | 0.3 |
| TK-DAY-LOCK-1-2 | D0LockGuard service-level 친화 메시지 (Clock 주입 KST, IntraDayLockGuardException) | 0.2 |
| TK-DAY-LOCK-1-3 | IT — D-0 UPDATE 차단 / override_reason 경로 통과 / D-1 UPDATE 통과 | 0.3 |

### ST-DAY-LOCK-2 — createdBy actor 책임 완성

| Task | 내용 | SP |
|---|---|:--:|
| TK-DAY-LOCK-2-1 | GreedyRotationAllocator — `new VcSchedule(...)` 직후 `assignCreatedBy(actor)` 호출. actor 는 AllocationContext 의 신규 field `requestedBy` (PLANNER 사번) | 0.3 |
| TK-DAY-LOCK-2-2 | VC chain Listener (OrderCommittedListener) — chain 진입 시 actor 전달 (orderCommitted.committedBy → Allocator) | 0.3 |
| TK-DAY-LOCK-2-3 | SwapProposalService / IntraDayOverrideService — 신규 INSERT 경로 actor 명시 | 0.2 |
| TK-DAY-LOCK-2-4 | IT — 모든 INSERT 경로 createdBy NOT NULL 회귀 (Allocator/Listener/Swap/Override) | 0.2 |

### ST-DAY-LOCK-3 — MES 인터페이스 + DegradedModeService

| Task | 내용 | SP |
|---|---|:--:|
| TK-DAY-LOCK-3-1 | `MesShiftPort` 인터페이스 (`reportProduction(machineId, shift, qty, timestamp)` + `lastReceivedShift(machineId)`) — Sprint 17 baseline 은 in-memory stub impl | 0.3 |
| TK-DAY-LOCK-3-2 | DegradedModeService — 1 shift (8h) 미수신 임계 감지 + Redis flag (`mes.degraded.{machineId}`) + 자동 해제 (MES 정상 수신 시) | 0.4 |
| TK-DAY-LOCK-3-3 | V044 — `mes_shift_event` 테이블 (machine_id, shift_no, planned_qty, actual_qty, received_at, source ∈ {MES, EXCEL_FALLBACK}) + audit | 0.3 |
| TK-DAY-LOCK-3-4 | Slack/STOMP 알림 — degraded mode 진입/해제 시 (REQ-FUNC-OC-009 정합) | 0.2 |

### ST-DAY-LOCK-4 — Excel 폴백 endpoint + Frontend UI

| Task | 내용 | SP |
|---|---|:--:|
| TK-DAY-LOCK-4-1 | `POST /api/v1/mes/shift/fallback` — Excel upload (machine, shift, qty) 수동 입력 endpoint (PLANNER + IT_OPS RBAC) | 0.3 |
| TK-DAY-LOCK-4-2 | Frontend — VcSimulationPage 상단 degraded mode 배너 (red Tag + "MES 미수신 N shift, 폴백 모드") + Excel upload 버튼 (Modal) | 0.3 |

### ST-DAY-LOCK-5 — EP-DAY-LOCK IT 5+ cases + 회귀

| Task | 내용 | SP |
|---|---|:--:|
| TK-DAY-LOCK-5-1 | IT 5 — D-0 lock + createdBy actor + MES degraded + Excel fallback + 회귀 | 0.3 |
| TK-DAY-LOCK-5-2 | 기존 VcConfirmGate/IntraDayLock/Allocator IT 회귀 GREEN | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ D-0 row UPDATE 시도 → 423 + 한국어 (BR-V07 강화)
2. ✅ override_reason 비-NULL → D-0 UPDATE 통과 (BR-V07 정합)
3. ✅ 모든 신규 VcSchedule row 에 createdBy NOT NULL (Allocator/Listener/Swap/Override)
4. ✅ MES 1 shift 미수신 → degraded mode + Slack/STOMP 알림 + Redis flag
5. ✅ Excel 폴백 upload → mes_shift_event source='EXCEL_FALLBACK' 기록 + audit
6. ✅ MES 정상 수신 시 degraded mode 자동 해제
7. ✅ Frontend degraded mode 배너 + Excel upload Modal

**비기능 DoD:**
1. ✅ ArchUnit GREEN (MES 모듈 boundary)
2. ✅ Backend 신규 IT 5+ + 회귀 0
3. ✅ TypeScript compile + frontend unit tests GREEN
4. ✅ V043/V044 migration 적용 (DEV/Testcontainers 부팅 정상)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| BR-V07 D-0 trigger 가 V039 sample row UPDATE 차단 (sample row 갱신 필요 시) | V039 sample 갱신 불가 | Sample row 의 production_date 는 항상 D-3 이상 (LocalDate.now() 기반 seed 는 향후 hotfix 검토) |
| MES 미연결 = 영구 degraded mode 진입 → 신호 노이즈 | UI 배너 상시 표시 | Sprint 17 baseline 은 MES `enabled` flag (`scheduling.mes.enabled=false` default) — 실 MES 미연동 시 degraded 알림 비활성 |
| Allocator/Listener actor 누락 → createdBy NULL 잔존 | dual-review legacy fallback 의존 지속 | Sprint 17 IT 가 createdBy NOT NULL 강제 회귀 — Sprint 18 EP-NOTIFY 진입 게이트 |
| Excel 폴백 upload 동시성 (여러 PLANNER 동시 upload) | shift_event 중복 | (machine_id, shift_no) UNIQUE constraint + ON CONFLICT DO UPDATE (latest source) |
| V043 trigger 가 confirmed_at/by audit 컬럼 UPDATE 차단 | confirmedAt 갱신 불가 | trigger allowlist — override_reason/override_by + confirmed_at/by + updated_at 만 허용 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — DB trigger + Service guard + Allocator actor:
1. TK-DAY-LOCK-1-1~3 (BR-V07 D-0 강화)
2. TK-DAY-LOCK-2-1~4 (createdBy actor 완성)

**Day 2** — MES 인터페이스 + degraded mode:
3. TK-DAY-LOCK-3-1~4 (MES port + DegradedModeService + V044)
4. TK-DAY-LOCK-4-1~2 (Excel fallback endpoint + Frontend 배너)

**Day 3** — IT + DoD:
5. TK-DAY-LOCK-5-1~2 (EP-DAY-LOCK IT + 회귀)
6. **DoD 본 PC 시각 검증** — D-0 UPDATE 차단 + MES degraded 시뮬

**총 ~2 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Migration | V043 (D-0 lock trigger), V044 (mes_shift_event table) |
| Backend Service | D0LockGuard, MesShiftPort + InMemoryStub, DegradedModeService, MesFallbackController |
| Backend IT | `DayLockIT.java` (5 cases) + Allocator/Listener actor 회귀 |
| Frontend | VcSimulationPage degraded mode 배너 + ExcelFallbackModal 컴포넌트 |
| Docs | rbac-matrix.md v1.3 부분 갱신 (POST /api/v1/mes/shift/fallback PLANNER+IT_OPS) |

---

## 9. Sprint 17 후 다음 단계

**Sprint 18 (EP-NOTIFY) 진입 조건:**
- ✅ DoD 11/11 충족
- ✅ 본 PC D-0 락 시각 검증 (override 경로 포함)
- ✅ MES degraded mode 진입/해제 시뮬 (`scheduling.mes.enabled=true` + 1 shift 강제 미수신)

**Sprint 18 첫 작업** — PLAN-SPRINT-18 작성 (운영 알림 통합 — Slack/STOMP/Kakao 일원화 + Critical Diff escalation + Capa overflow 알림).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-DAY-LOCK 5 Story / 16 Task / ~4 SP 분해 + 의존성 DAG + DoD 11 + 3-Day 작업 순서. Sprint 4 EP-13 BR-V07 자산 + Sprint 16 EP-CONFIRM 자산 (V041/V042 + dual-review) 위에 D-0 락 강화 + MES 폴백 + createdBy actor 완성. MES 는 stub 인터페이스 + degraded mode 감지 + Excel 수동 upload baseline (실 MES 연동은 Sprint 19+/Phase 5 carry-over). |
