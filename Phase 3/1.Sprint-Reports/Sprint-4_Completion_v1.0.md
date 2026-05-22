# Sprint 4 완료 보고서 (Phase 3 Sprint 4 종료 게이트)

**Sprint**: S4 | **기간**: 2026-05-22 (1일 · AI 가속 압축) | **상태**: ✓ 완료
**작성**: 2026-05-22 | **결재**: 작성 — Claude Code, 검토 대기 — STK-01 + STK-08

> Sprint 3 (압출 EX 6 Epic 27 Task) 종료 직후 진입. **거버넌스 + 당일 락 + 라우팅 + Excel
> export + cascade PUSH** 7 Epic 통합 — Sprint 4 단일 최중요 과제 (BR-X01·X02·V07).
> 본 보고서는 Sprint Review 데모 가능 시점 기준.

---

## 1. Sprint 4 목표 (Sprint-4_EntryPlan_v1.0 §1)

> "거버넌스·최적화·당일 락 — BR-X01 (Confirmed) + BR-X02 (audit) + BR-V07 (일중 락) +
>  BR-E08 (NS-S09 신규 라인) + BR-E09 (Excel 시트명) + BR-X03·E11 (cascade replan) +
>  REQ-FUNC-EX-014 (WebSocket PUSH p95 ≤ 2초). 7 Epic ~32 SP."

핵심 KPI — REQ-FUNC-VC-019·020 + EX-013·014·018 + NFR-SEC-004 + NS-S09.

---

## 2. Task 매트릭스 (16 commit, 7 Epic 100% 완료)

### EP-10 사용자 확정 게이트 (BR-X01) — 2 Story 6 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-10-1 VC Confirm | TK-10-1-1+2+3 V022 vc_schedule confirm 트리거 + VcScheduleConfirmationService + Controller + IT 5 + 단위 4 | ✓ | 9678df4 |
| ST-10-2 EX Confirm | TK-10-2-1+2+3 V023 ex_candidate confirm 트리거 + ExCandidateConfirmationService + Controller + ExConfirmedEvent + IT 4 + 단위 4 | ✓ | 517b629 |

### EP-11 Audit 기록 (BR-X02, NFR-SEC-004) — 2 Story 6 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-11-1 audit trigger | TK-11-1-1+2 V025 schedule_audit_log + 3 trigger (vc/ex/order) + @Auditable AOP + audit::aop NamedInterface | ✓ | 3d59b74 |
| ST-11-2 immutability | TK-11-2-1+2+3 V026 REVOKE UPDATE/DELETE/TRUNCATE + BEFORE 트리거 + audit_reader role | ✓ | 1b42198 |
| ST-11-1 적용 | TK-11-1-3 @Auditable VC/EX Confirm Service + AuditTriggerIT 5 (INSERT/reason/UPDATE·DELETE·TRUNCATE reject) | ✓ | 42172c9 |

### EP-12 Excel 역-Export (BR-E09) — 2 Story 6 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-12-1 master export | TK-12-1-1+2+3 MasterExcelExporter (VC_CONSTRAINT + LINE_TYPE + SETTING_GROUP) + ExportController + order→master 의존성 | ✓ | ce2bfcd |
| ST-12-2 matrix export | TK-12-2-1+2+3 ExtrusionMatrixExporter (M월d일(압출) 정규식) + ExMatrixExportController + 단위 8 | ✓ | c96b296 |
| ST-12-1+2 IT | TK-12-1+2 ExportControllerIT 6 (3 sheet 구조 + 47품번 + 4 라인 + 시트명 + yield + invalid range) | ✓ | 72dc40b |

### EP-13 v1.4 당일 락 (BR-V07) — 4 Story 7 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-13-1+2 lock | TK-13-1+2 V027 enforce_vc_intra_day_lock trigger + IntraDayLockRule + VcSchedule override 컬럼 + 도메인 단위 3 | ✓ | eadb913 |
| ST-13-3+4 override | TK-13-3+4 IntraDayOverrideService @Auditable + BusinessDayBoundaryFormatter (YYYY-MM-DD_END) + 단위 5 + IntraDayLockIT 5 | ✓ | 1534a60 |

### EP-14 신규 라인 우선 라우팅 (BR-E08, NS-S09) — 1 Story 3 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-14-1 (master) | TK-14-1-1 V024 line_type 4 라인 seed + line_product_compatibility + LineRoutingLookup facade | ✓ | 8e1fbff |
| ST-14-1 (정책) | TK-14-1-2+3 ExLineRoutingPolicy (NEW priority ASC 우선 + FORD fallback) + 단위 5 + LineRoutingIT 7 | ✓ | 34550b7 |

### EP-EX13 partial replan 정식 활성 (BR-X03·E11) — Sprint 3 carry-over

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-EX13-1 정식 | TK-EX13-1-3 ExScheduleCandidate.applyVcChange + PartialReplanService.replanWithContext (QUANTITY/DATE/DELETED/CONFIRMED 차단) + @Auditable + PartialReplanCascadeIT 7 (100건 시뮬) | ✓ | 0b480af |

### EP-EX14 WebSocket PUSH (REQ-FUNC-EX-014) — 1 Story 3 Task

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-EX14-1 event | TK-EX14-1-1 ExReplanCompletedEvent (ex.events) + ApplicationEventPublisher 주입 | ✓ | 5133bc6 |
| ST-EX14-1 listener | TK-EX14-1-2 ExReplanPushListener @ApplicationModuleListener + SimpMessagingTemplate /topic/extrusion-updates + 단위 2 | ✓ | 2aebd04 |
| ST-EX14-1 IT + p95 | TK-EX14-1-3 ExReplanPushIT 3 (chain + no-push + **p95 30회 ≤ 2,000ms 측정 통과**) | ✓ | eaf9483 |

**합계** — Epic 7 / Story 12 / Task ~40 (모두 Must) — **100% 완료**.

---

## 3. 핵심 지표 (KPI 달성)

| 영역 | 지표 | 목표 | 실측 | 상태 |
|---|---|---|---|:--:|
| **🌟 EP-10 Confirmed 게이트** | DB 직접 쓰기 차단 | 100% (BR-X01) | 100% (trigger reject) ✅ | ✓ |
| **EP-10 RBAC** | ROLE_PLANNER 강제 | 100% | @PreAuthorize | ✓ |
| **🌟 EP-11 audit row 생성** | 모든 mutation | 100% (BR-X02) | 100% (3 trigger AFTER) ✅ | ✓ |
| **EP-11 immutability** | UPDATE/DELETE/TRUNCATE reject | 100% | 100% (REVOKE + BEFORE trigger) | ✓ |
| **EP-11 actor 캡쳐** | @Auditable → current_setting | 100% | 100% (SET LOCAL via JdbcTemplate) | ✓ |
| **EP-12 POI XSSF** | 셀-수준 차이 | ≤ 2% | 0% (header + value 정합) | ✓ |
| **EP-12 시트명 BR-E09** | `\d+월\d+일(압출)` 정규식 | 100% | 100% (4 case parameterized) | ✓ |
| **🌟 EP-13 일중 락** | 다른 angle INSERT 차단 | 100% (BR-V07) | 100% (trg_vc_intra_day_lock) ✅ | ✓ |
| **EP-13 override** | reason + actor 강제 | 100% | 100% (DB trigger + 도메인 invariant) | ✓ |
| **EP-13 DO-04 boundary** | YYYY-MM-DD_END | 4 case 정확 | 100% (Boundary Formatter) | ✓ |
| **🌟 EP-14 NS-S09 신규** | 사용률 | ≥ 90% | 100% (100 일반 hose L1 1순위) ✅ | ✓ |
| **EP-14 포드 전용** | 신규 시도 | 0건 | 0건 (ford_only filter) | ✓ |
| **EP-EX13 cascade** | 수동 호출 | 0건 | 0건 (Listener AFTER_COMMIT) | ✓ |
| **EP-EX13 100건 시뮬** | 모두 자동 | 100% | 100% (PartialReplanCascadeIT) | ✓ |
| **🌟 EP-EX14 p95** | PUSH 지연 | ≤ 2,000ms | ≤ 2,000ms (30회 측정) ✅ | ✓ |
| **EP-EX14 chain** | replan → STOMP | AFTER_COMMIT + Async | 100% (Awaitility verify) | ✓ |
| **ArchUnit** | NamingConvention + KST + Layered + PreAuthorize + CalendarSingleSource | 0 위반 | 0 위반 (29 rule PASS) | ✓ |
| **Modulith verify** | 8 모듈 + audit::aop NamedInterface | 0 위반 | 0 위반 | ✓ |
| **전체 회귀** | 212+ tests | 0 failure | 0 failure | ✓ |

---

## 4. 신규 인프라 (Flyway V022~V027)

| Migration | 테이블 / VIEW | Epic·Task |
|---|---|---|
| V022 | ALTER `app.vc_schedule` (confirmed_at/by + enforce_vc_schedule_transition trigger) | EP-10 ST-10-1 |
| V023 | ALTER `app.ex_schedule_candidate` (confirmed_at/by + enforce_ex_candidate_transition trigger) | EP-10 ST-10-2 |
| V024 | `master.line_type` (4 row seed) + `master.line_product_compatibility` (ford_only) | EP-14 ST-14-1 |
| V025 | `audit` schema + `audit.schedule_audit_log` + 3 trigger (vc/ex/order) + fn_capture_mutation | EP-11 ST-11-1 |
| V026 | REVOKE UPDATE/DELETE/TRUNCATE ON audit.* + audit_reader role + BEFORE block trigger | EP-11 ST-11-2 |
| V027 | ALTER `app.vc_schedule` (override_reason/by) + enforce_vc_intra_day_lock trigger | EP-13 ST-13-1·3·4 |

**Trigger 누적 11종** — V022 vc 전이 + V023 ex 전이 + V025 audit 3 (vc/ex/order) + V026 audit
block 2 (UPDATE/DELETE + TRUNCATE) + V027 일중 락 + V014 vc_hose_rule LISTEN/NOTIFY +
V018·V020·V021 LISTEN/NOTIFY. application-level bypass 100% 차단.

---

## 5. 신규 모듈·패키지 (`com.scheduling.audit` 본격 활성 + 양 도메인 모듈 확장)

### `backend/audit/` (감사 모듈 — Sprint 4 본격 활성)

```
com.scheduling.audit/
  aop/             Auditable annotation + AuditableAspect (set_config audit.actor + audit.reason)
                   package-info @NamedInterface("aop") — vc/ex 모듈 import 허용
  resources/db/migration/  V025 schema + 3 trigger + V026 immutability + audit_reader role
```

### `backend/vc/` 확장 — Confirmed 게이트 + override

```
com.scheduling.vc/
  confirm/         VcScheduleConfirmationService @Auditable + VcConfirmController @PreAuthorize PLANNER
  domain/          + confirm() + applyOverride() 도메인 메서드 (BR-X01·V07 invariant)
  rule/            + IntraDayLockRule (RuleEngine 5번째 — BR-V07 Allocator pre-check)
  override/        + IntraDayOverrideService @Auditable + BusinessDayBoundaryFormatter (DO-04 YYYY-MM-DD_END)
  resources/db/migration/  V022 vc_schedule 상태 머신 + V027 일중 락 trigger
```

### `backend/ex/` 확장 — Confirmed + LineRouting + cascade replan + PUSH event

```
com.scheduling.ex/
  confirm/         ExCandidateConfirmationService @Auditable + ExConfirmController @PreAuthorize
  events/          + ExConfirmedEvent + ExReplanCompletedEvent (ex::events NamedInterface)
  routing/         + ExLineRoutingPolicy (BR-E08 NEW priority ASC + FORD fallback)
  event/           + PartialReplanService.replanWithContext (Sprint 3 stub 확장 — QUANTITY/DATE/DELETED)
  export/          + ExtrusionMatrixExporter + ExMatrixExportController (BR-E09 M월d일(압출))
  resources/db/migration/  V023 ex_candidate 상태 머신
```

### `backend/master/` 확장 — LineRouting facade

```
com.scheduling.master/
  api/             + LineTypeSummary + LineRoutingLookup
  line/            LineType + LineProductCompatibility (@IdClass composite PK) + Repositories + LookupImpl
  resources/db/migration/  V024 line_type + line_product_compatibility
```

### `backend/order/` 확장 — Excel export

```
com.scheduling.order/
  export/          + MasterExcelExporter (3 sheet POI XSSF) + ExportController @PreAuthorize PLANNER+IT_OPS
```

### `backend/notify/` 확장 — EX cascade PUSH

```
com.scheduling.notify/
  + ExReplanPushListener @ApplicationModuleListener (AFTER_COMMIT + Async)
    → SimpMessagingTemplate /topic/extrusion-updates
```

**ArchUnit 통과** — audit::aop NamedInterface 신규 + vc/ex allowedDependencies 갱신.
notify allowedDependencies = common + order::events + vc::events + ex::events.

---

## 6. 거버넌스 종단 체인 (Sprint 4 핵심 deliverable)

```
[Planner UI confirm] (Sprint 5 진입 예정)
  ↓ POST /api/v1/schedule/vc/{id}/confirm @PreAuthorize PLANNER
[VcScheduleConfirmationService @Auditable]
  ↓ VcSchedule.confirm() (CANDIDATE → CONFIRMED, audit set)
[DB trigger trg_vc_schedule_transition] (V022 — 잘못된 전이 reject)
  ↓
[DB trigger trg_audit_vc_schedule] (V025 — audit row 자동 발행)
[audit.schedule_audit_log] (V026 REVOKE — UPDATE/DELETE 불가)

[BR-V07 일중 앵글 교체 시]
  ↓ POST IntraDayOverrideService.applyOverride(reason)
[DB trigger trg_vc_intra_day_lock] (V027 — reason/by 누락 reject)
  ↓
[audit row reason="BR-V07 일중 앵글 교체 override"]

[BR-X03 cascade]
[VcChangedEvent] (Modulith @ApplicationModuleListener AFTER_COMMIT + Async)
  ↓ VcChangedListener
[PartialReplanService.replanWithContext] (정식 활성 — Sprint 4)
  ↓ ExScheduleCandidate.applyVcChange (yield + deadline 갱신)
[ExReplanCompletedEvent]
  ↓ ExReplanPushListener (notify 모듈)
[SimpMessagingTemplate.convertAndSend /topic/extrusion-updates] (p95 ≤ 2,000ms)
[현장 압출 패드] (SockJS subscribe)
```

---

## 7. 발견 production domain bug / 명세 모순 0건

Sprint 4 는 신규 인프라 (audit + trigger + cascade event) 가 다수였으나 **명세 모순 0건**.
다만 **테스트 fixture 보정 1건**:
- `ValidateAllPerformanceIT.seed` — rotation 별 다른 angle 시드가 신규 BR-V07 trigger 와 충돌 → slot 별 단일 angle 로 fixup (`ANGLE-29673-S<slot>` 패턴)

**기술 이슈 해결 3건**:
- PL/pgSQL `TG_OP/OLD/NEW` 는 trigger 함수에서만 접근 가능 → helper 함수 인라인 전략
- Postgres P0001 RAISE → Spring 이 `UncategorizedSQLException` 으로 변환 (DataIntegrityViolationException 아님) → `DataAccessException` 부모로 assertion
- V026 immutability 가 의도대로 `@BeforeEach DELETE FROM audit.*` 차단 → UUID 격리 + cleanup 제거

---

## 8. 16 Commit 시간순 정리 (Sprint 4 전체)

```
9678df4  TK-10-1-1+2+3 VC Confirm 게이트 — Planner 확정 + DB trigger (BR-X01)
517b629  TK-10-2-1+2+3 EX Confirm 게이트 — SCHEDULED → CONFIRMED + 이벤트 (BR-X01)
8e1fbff  TK-14-1-1 라인 라우팅 마스터 — line_type + 호환성 + facade (BR-E08)
34550b7  TK-14-1-2+3 ExLineRoutingPolicy — NEW 우선·FORD fallback·포드 전용 차단 (NS-S09)
3d59b74  TK-11-1-1+2 V025 audit triggers + @Auditable AOP (BR-X02)
1b42198  TK-11-2-1+2+3 V026 audit immutability — REVOKE + audit_reader role (NFR-SEC-004)
42172c9  TK-11-1-3 @Auditable 적용 — VC/EX Confirm services + AuditTriggerIT (BR-X02)
eadb913  TK-13-1+2 V027 일중 락 trigger + IntraDayLockRule + override 컬럼 (BR-V07)
1534a60  TK-13-3+4 OverrideService + 영업일 경계 키 + IntraDayLockIT (BR-V07)
ce2bfcd  TK-12-1-1+2+3 MasterExcelExporter — VC_CONSTRAINT/LINE_TYPE/SETTING_GROUP
c96b296  TK-12-2-1+2+3 ExtrusionMatrixExporter — 일별 시트 M월d일(압출) (BR-E09)
72dc40b  TK-12-1+2 ExportControllerIT — POI XSSF 셀-수준 회귀 (TC-OC-013, TC-EX-018)
0b480af  TK-EX13-1-3 PartialReplan 정식 활성 — replanWithContext + applyVcChange (BR-X03·E11)
5133bc6  TK-EX14-1-1 ExReplanCompletedEvent — partial replan 후 발행 (REQ-FUNC-EX-014)
2aebd04  TK-EX14-1-2 ExReplanPushListener — STOMP /topic/extrusion-updates (BR-EX14)
eaf9483  TK-EX14-1-3 ExReplanPushIT — chain + p95 ≤ 2,000ms (REQ-NF-PER-004)
```

---

## 9. Sprint 4 Velocity

- **계획**: 32 SP (EntryPlan critical path = EP-10·11·13 18 SP + 잔여 14 SP)
- **실제 완료**: ~32 SP (EP-10 5 + EP-11 5 + EP-12 5 + EP-13 8 + EP-14 3 + EP-EX13 3 + EP-EX14 3)
- **실제 PD**: 1일 (AI 가속) → ~22.4 PD 압축률 ≈ 22배 (Sprint 3 17배 대비 가속)
- **병렬 작업 활용**: 4 turn 중 4 turn 병렬 (EP-10 + EP-14 / EP-11 + EP-13 / EP-12 + EP-EX13 / EP-EX14 단독)
- **누적 commit 95건** (Sprint 0~4 — 47 / 25 / 18 / 20 / 16)

---

## 10. Sprint 5 진입 게이트 충족

- [x] **7 Epic 100% 완료** (EP-10·11·12·13·14·EX13·EX14)
- [x] **거버넌스 4-layer 통과** (DB trigger + AOP @Auditable + RBAC @PreAuthorize + audit immutability)
- [x] **BR-X01·X02·V07 hard 강제** (직접 DB 쓰기 / audit bypass / 일중 교체 모두 100% 차단)
- [x] **NS-S09 신규 라인 우선 100%** (포드 전용 0건)
- [x] **REQ-FUNC-EX-014 p95 PUSH ≤ 2,000ms** (30회 측정 통과)
- [x] **Modulith verify 0 위반** (8 모듈 + audit::aop NamedInterface 신규)
- [x] **ArchUnit 29 rule 통과** (NamingConvention + KST + Layered + PreAuthorize + CalendarSingleSource)
- [x] **회귀 ≥ 99%** (212+ tests · 0 failure · 0 error)
- [x] **AI harness 안정** (16 commit · ~40 Task · 머지 충돌 0)

→ **Sprint 5 진입 승인 가능** (UI 통합 + AG Grid + React/Vite + Phase 2 본격 진입).

---

## 11. 차순위 carry-over (Sprint 5 이후)

| 항목 | 분류 | 이동 Sprint |
|---|---|---|
| Frontend React/Vite + AG Grid Enterprise (1500 row × 30 col) | UI | Sprint 5 |
| ExtrusionPad SockJS subscribe (현장 압출 패드 UI) | UI | Sprint 5 |
| OverrideModal — BR-V07 사유 입력 UI | UI | Sprint 5 |
| Redis Pub/Sub fallback (다중 인스턴스 확장 대비) | 인프라 | Sprint 6+ |
| spring-modulith-events-jpa 영속 publication (장애 복구) | 이벤트 인프라 | Sprint 5 |
| audit.schedule_audit_log 월별 파티셔닝 (3년 보존 NFR-SEC-004) | 운영 | Sprint 6+ |
| EX_CONSTRAINT seed 47품번 전체 speed/length 확장 | 마스터 데이터 | Sprint 5 (운영팀 정합) |
| k6 부하 테스트 (1500 row p95 < 800ms 본격) | 성능 | Sprint 5~6 |

---

## 12. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — Sprint 4 (16 commit, 7 Epic 100% 완료, BR-X01·X02·V07 hard 강제, p95 PUSH ≤ 2,000ms ✅) |
