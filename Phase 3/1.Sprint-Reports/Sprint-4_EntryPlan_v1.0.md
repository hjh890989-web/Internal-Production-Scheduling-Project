# Sprint 4 진입 계획 (거버넌스·최적화·당일 락)

**Sprint**: S4 | **목표 기간**: 2026-05-22 ~ (2주, AI 가속 시 ~1~2일) | **상태**: 🔄 진입 게이트
**작성**: 2026-05-22 | **상위 참조**: [Sprint-3_Completion_v1.0.md](Sprint-3_Completion_v1.0.md) §10·11, [WBS v1.2 §7](../../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)

> Sprint 3 (압출 EX 6 Epic 27 Task) 종료 직후 진입. **압출 종단 파이프라인 완성**
> (D-1 → yield BR-E05=2531 → grouping → gate → conflict). Modulith verify 0 위반.
> Sprint 4 = **거버넌스·최적화·당일 락** — BR-X01·X02·V07 + 라우팅 정책 + Excel 역-export.

---

## 1. Sprint 4 목표 (PDD-MASTER v1.7 + SRS v1.5 § 거버넌스·v1.4)

- **EP-10 사용자 확정 게이트** (M-10) — Candidate → Confirmed 상태 머신 + BR-X01 hard.
- **EP-11 Audit 기록** (M-11) — DB 트리거 + AOP `@Auditable` + NFR-SEC-004 3년 보존.
- **EP-12 Excel 역-Export** (M-12) — POI XSSF + 압출 시트명 정규식 (BR-E09).
- **EP-13 v1.4 당일 락** (Sprint 4 핵심 ⭐⭐) — BR-V07 + 4-layer 강제 (ADR-016) + override 모달.
- **EP-14 신규 라인 우선 라우팅** (S-02) — 신규 90%↑ + 포드 fallback (BR-E08).
- **EP-EX13 carry-over 정식 활성** — partial replan 본격 실행 (yield + grouping 재실행).
- **EP-EX14 WebSocket PUSH** — STOMP + Redis Pub/Sub fallback (REQ-FUNC-EX-014).

---

## 2. Sprint 4 Epic·SP 매트릭스

| Epic | 제목 | SP | 의존 (선행) | 핵심 산출 |
|---|---|:--:|---|---|
| **EP-10** ⭐ | 사용자 확정 게이트 (Candidate→Confirmed) | 5 | EP-05 ✓ EP-07 ✓ | `ScheduleConfirmationService` + 상태 머신 + Planner role 강제 |
| **EP-11** ⭐ | Audit 기록 (DB 트리거 + AOP) | 5 | EP-10 | `@Auditable` AOP + audit DB 트리거 + 3년 보존 (NFR-SEC-004) |
| **EP-12** ⭐ | Excel 역-Export | 5 | EP-10 (Confirmed schedule) | POI XSSF + MASTER export + 압출 매트릭스 시트명 정규식 |
| **EP-13** ⭐⭐ | v1.4 당일 락 강제 | 8 | EP-10·EP-11 | 4-layer (DB UNIQUE + RuleEngine + 일말 경계 + override 모달) |
| **EP-14** ⭐ | 신규 라인 우선 라우팅 | 3 | EP-EX11 ✓ | `ExLineRoutingPolicy` + 90%↑ KPI + 포드 fallback |
| **EP-EX13** (carry-over) | partial replan 본격 활성 | 3 | EP-10 + EP-EX11 ✓ | yield + grouping 재실행 (Sprint 3 기반 구조 위에 알고리즘) |
| **EP-EX14** | WebSocket PUSH (압출 패드) | 3 | EP-EX13 | STOMP @ /ws + Redis Pub/Sub fallback + soak 테스트 |

**합계**: **~32 SP** (Sprint 4 capacity 30 PD = 50 SP velocity 기준 · ~64% 활용).
EP-10 → EP-11 → EP-13 가 critical path (18 SP, Sprint 4 DoD).

---

## 3. 의존성 그래프

```
Sprint 3 (압출 종단)
       │
       ├──► EP-10 (Confirmed 게이트) ⭐
       │      │
       │      ├──► EP-11 (Audit 기록) ⭐
       │      │      │
       │      │      └──► EP-13 (당일 락 4-layer) ⭐⭐
       │      │
       │      ├──► EP-12 (Excel 역-Export) ⭐
       │      │
       │      └──► EP-EX13 정식 활성 (carry-over)
       │             │
       │             └──► EP-EX14 (WebSocket PUSH)
       │
       └──► EP-14 (신규 라인 우선 라우팅)
```

Critical Path: **EP-10 → EP-11 → EP-13** (~18 SP, ~13 PD).

---

## 4. 권장 진행 순서 (AI 가속 vibe coding)

| 단계 | Epic·Story | 비고 |
|---|---|---|
| **Phase A** (Day 1) | EP-10 ST-10-1 (VC Candidate→Confirmed) + ST-10-2 (EX 확정) | Planner role 강제 + 상태 머신 |
| **Phase B** (Day 1~2) | EP-11 ST-11-1 (DB 트리거 audit) + ST-11-2 (불변성 강제) | `@Auditable` AOP + INSERT-only |
| **Phase B** (Day 1~2) | EP-12 ST-12-1·2 (Excel export — MASTER + 압출 매트릭스) | POI XSSF + 시트명 정규식 |
| **Phase C** (Day 2) | EP-13 ST-13-1~5 (당일 락 4-layer) ⭐⭐ | DB UNIQUE + RuleEngine + 일말 + override 모달 |
| **Phase D** (Day 2) | EP-14 ST-14-1 (라인 라우팅 + 포드 fallback) | 90%↑ KPI |
| **Phase E** (Day 2~3) | EP-EX13 정식 활성 (replan 실 알고리즘) + EP-EX14 (WebSocket PUSH) | Sprint 3 기반 위에 알고리즘 |
| **Phase F** (Day 3) | Sprint 4 회고 + Sprint 5 진입 plan | |

**병렬 옵션** (의존성 그래프 기반):
- **A. EP-10 + EP-12 병렬** — Confirmed 게이트 vs Excel export (다른 도메인)
- **B. EP-11 + EP-12 병렬** — Audit AOP vs Export (다른 모듈)
- **C. EP-13 단독** — 당일 락 4-layer 는 큰 작업 (8 SP), 별도 turn 권장
- **D. EP-14 + EP-EX13/14 병렬** — 라우팅 정책 vs Replan/PUSH (다른 모듈)

---

## 5. 신규 데이터베이스 마이그레이션 (예상 V022~V026)

| Migration | 테이블 / VIEW | Epic·Task |
|---|---|---|
| V022 | ALTER `app.vc_schedule` (status state machine + confirmed_at + confirmed_by) | EP-10 ST-10-1 |
| V023 | ALTER `app.ex_schedule_candidate` (confirmed_at + confirmed_by + lock_until_eod) | EP-10 ST-10-2 |
| V024 | `audit.schedule_audit` 통합 + DB 트리거 (INSERT-only) + REVOKE UPDATE/DELETE | EP-11 ST-11-1·2 |
| V025 | ALTER vc_schedule + ex_candidate (intra_day_lock_marker + override_reason) | EP-13 ST-13-1 |
| V026 | `app.line_routing_policy` (신규/포드 라우팅 + KPI 카운터) | EP-14 ST-14-1 |

---

## 6. 신규 모듈 활성 (`com.scheduling.audit` 본격 + `com.scheduling.notify` 확장)

### `backend/audit/` (감사 모듈 — Sprint 4 본격 활성)

```
com.scheduling.audit/
  domain/           ScheduleAudit + AuditAction enum (CONFIRM/UPDATE/DELETE/OVERRIDE)
  service/          AuditableAspect (AOP @Auditable) + AuditPublisher
  repository/       ScheduleAuditRepository (INSERT-only + REVOKE)
  events/           ScheduleAuditedEvent (notify 구독)
```

### `backend/notify/` 확장 — WebSocket PUSH

```
com.scheduling.notify/
  websocket/        StompConfig + ExtrusionPadController + Redis Pub/Sub fallback
  channels/         /topic/extrusion/pad/{lineCode} + soak 테스트
```

### `backend/vc/` + `backend/ex/` 확장

```
com.scheduling.vc/schedule/
  + ScheduleConfirmationService (Candidate→Confirmed 상태 머신, EP-10)
  + IntraDayLockRule (BR-V07 4-layer, EP-13)
  + OverrideService (사유 강제 입력, EP-13)
com.scheduling.ex/event/
  + PartialReplanService 본격 활성 (yield + grouping 재실행, EP-EX13)
```

### `backend/order/` 확장 — Excel export

```
com.scheduling.order/export/
  + ExcelExporter (POI XSSF + MASTER 통합 + 압출 매트릭스 시트명 정규식)
```

---

## 7. Sprint 4 DoD (진입 게이트 충족 → 종료 게이트 목표)

| 영역 | 지표 | 목표 |
|---|---|---|
| **EP-10 Confirmed 게이트** | DB 직접 쓰기 차단 | 100% (BR-X01) |
| **EP-11 Audit** | 모든 mutation audit row 생성 | 100% (BR-X02) |
| **EP-11 불변성** | UPDATE/DELETE 거부 | 100% (REVOKE 강제) |
| **EP-12 Export** | MASTER + 압출 매트릭스 셀-수준 차이 | ≤ 2% (REQ-FUNC-OC-013) |
| **EP-13 당일 락** | 일중 셋팅 교체 1주 회귀 | 0건 (BR-V07 hard) |
| **EP-13 override** | 사유 강제 입력 + audit | 100% |
| **EP-14 신규 라인** | 사용률 | ≥ 90% (NS-S09) |
| **EP-14 포드 전용** | 신규 시도 | 0건 (BR-E08) |
| **EP-EX13 replan** | vc.changed → yield/grouping 재실행 자동화 | 100% (수동 0건) |
| **EP-EX14 WebSocket** | p95 PUSH 지연 | ≤ 2초 (REQ-FUNC-EX-014) |
| **Modulith verify** | 8 모듈 + audit 본격 활성 | 0 위반 |
| **회귀** | DS-AUDIT-100 + DS-LOCK-7DAY + DS-EXPORT-CELL | 100% |

---

## 8. v1.4 SRS 통합 완성 (Sprint 4 핵심)

Sprint 2 v1.4 5종 제약 (EP-21) 완료 → Sprint 4 v1.4 거버넌스 부분 완성:
- **BR-V07 당일 락** — EP-13 4-layer 강제
- **BR-X01 사용자 확정** — EP-10 Planner role 강제
- **BR-X02 mutation audit** — EP-11 트리거 + AOP
- **BR-X05 Dual-review** — Sprint 2 carry-over (EP-34) → EP-13 거버넌스 묶음 흡수

v1.4 신규 SRS REQ-FUNC 통합 (Sprint 4 마감 시점):
- REQ-FUNC-VC-012·013·014 — EP-13 당일 락
- REQ-FUNC-VC-019·EX-019 — EP-10 Confirmed
- REQ-FUNC-VC-020·EX-020 — EP-11 Audit

---

## 9. 진입 게이트 체크리스트 (Sprint 3 완료 → Sprint 4 진입)

- [x] **Sprint 3 6 Epic 완료** (EP-07·08·09·EX11·EX12 + EP-EX13 기반 구조) — Sprint-3_Completion §10
- [x] **압출 종단 파이프라인** (D-1 → yield BR-E05=2531 → grouping → gate → conflict) ✅
- [x] **vc.confirmed 이벤트 인프라** (TK-07-1-1 Modulith) — EP-10 본격 활성 토대
- [x] **vc.changed 이벤트 기반 구조** (TK-EX13-1-1+2+3) — EP-10 완료 후 정식 활성
- [x] **마스터 5개 풀 확장** (V017~V021) — shift, ex_constraint full, inventory, setting_group
- [x] **Modulith verify 0 위반** + CalendarSingleSourceArchTest (CON-10) 추가
- [x] **AI harness 안정** (20 commit · 27 Task · 머지 충돌 0)

→ **Sprint 4 진입 승인 가능**. Phase A (EP-10 Confirmed 게이트) 즉시 시작 가능.

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — Sprint 4 진입 계획 (EP-10·11·12·13·14·EX13·EX14 = ~32 SP, critical path 18 SP) |
