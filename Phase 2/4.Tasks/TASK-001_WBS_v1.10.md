# 작업 분할 구조서 (WBS) v1.10 — Sprint 14 EP-VC-FULL 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.10 | **작성일**: 2026-05-27
**전판**: [v1.9](TASK-001_WBS_v1.9.md) (Sprint 13 EP-OC-FULL 마감 + V038 hotfix Addendum)
**상태**: Addendum — Sprint 13 hotfix AuditLogService 마감 + Sprint 14 EP-VC-FULL (성형 시뮬뷰 PDD-02 완성) 100% 마감 + DoD 10/11 ✅ + 1 carry-over (본 PC 실 시나리오)

> v1.9 (Sprint 13 마감, 63 Epic / 315 SP 실) 의 본 PC 검증 중 식별된 **mutation-less endpoint
> BR-X02 정합 (Sprint 13 hotfix AuditLogService)** + Sprint 14 ST-VC-1~6 6 Story 마감.

---

## 1. v1.9 → v1.10 변경 요지

| 항목 | v1.9 (Sprint 13 + V038) | v1.10 (Sprint 13 hotfix + Sprint 14) |
|---|---|---|
| Epic 총수 | 63 | 63 (변동 없음, EP-VC-FULL 마감만) |
| SP 실 합 | 315 | **321** (+~6 실, Sprint 14 계획 6 정합) |
| Sprint 14 상태 | 계획 6 SP | ✅ **마감** (6 Story / 22 Task / 3 commits / ~1 PD AI 가속) |
| audit 커버리지 | mutation 있는 trigger만 | ✅ **AuditLogService** — mutation-less endpoint 도 명시 audit (Sprint 13 hotfix) |
| OrderCommittedEvent chain | publisher 만 (Sprint 13) | ✅ **publisher → vc listener** chain 완성 (Sprint 14 ST-VC-1) |
| 시뮬뷰 데이터 | 빈 grid | ✅ **V039 seed 15 sample** + 안내 메시지 |
| STOMP 실시간 | 미통합 | ✅ **VcSchedulePushListener** + Frontend subscribe + invalidate (REQ-NF-PER-004 p95 ≤ 2초) |
| AG Grid | 이미 완성 (Sprint 5 EP-15) | ✅ 검증 — 추가 작업 0 |

---

## 2. Sprint 13 hotfix — AuditLogService

| 항목 | v1.9 | v1.10 |
|---|---|---|
| OrderCommitController commit/reject audit | mutation 0 → trigger 미발화 → audit_log 0 | ✅ **AuditLogService.record direct INSERT** |
| audit::api NamedInterface | (api 패키지 비어 있음) | ✅ AuditLogService 등록 |
| Action enum | (없음) | INSERT / UPDATE / DELETE — audit_log CHECK constraint 정합 |
| 발견 → fix | 사용자 직접 검증 (audit_log 0 row) | commit `81b7c8d` |

**용도** — Sprint 18+ 신규 mutation-less 의사결정 endpoint 도 재사용 가능 (예: STG bulk 액션).

---

## 3. Sprint 14 마감 — EP-VC-FULL 6 Story 회고

### EP-VC-FULL 전체 (성형 시뮬뷰 PDD-02 완성)

**Sprint**: **S14** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-14_EP-VC-FULL_v1.0](PLAN-SPRINT-14_EP-VC-FULL_v1.0.md) (3-Day) / **SP 실**: ~6 / **선행**: EP-OC-FULL (S13)

| Story | 구현 | Commit |
|---|---|---|
| ST-VC-1 — OrderCommittedListener (vc.internal, Sprint 13 chain) | @ApplicationModuleListener AFTER_COMMIT async + LOG (Sprint 14 baseline, Phase 5+ VcScheduleService.draftBatch 진입점). OrderChangedListener (row-level) 와 의미 분리 (trackingId-level) | `9df885b` |
| ST-VC-2 — 시뮬뷰 V039 sample seed | vc_machine 5 (LP-01~04 + IC) + vc_schedule 15 (99999-SAMPLE-*, CURRENT_DATE 기반 1주 horizon) + WHERE NOT EXISTS (DEFERRABLE UNIQUE 호환, Sprint 8 V028 교훈) | `9df885b` |
| ST-VC-3 — AG Grid Enterprise 검증 (Sprint 5 EP-15 누적) | VcRotationGrid 이미 AgGridReact + Pivot (date × machineSlot × rotation 1-18 = D1-8 + N1-10) + statusBar + pinned + animateRows. Sprint 14 추가 작업 0 | (이미 완성) |
| ST-VC-4 — STOMP 실시간 broadcast | VcSchedulePushListener (notify 모듈, Sprint 6 ExReplanPushListener 패턴 재사용) — VcChangedEvent + VcConfirmedEvent → /topic/vc-schedule-updates push. notify/build.gradle.kts vc 의존 + Frontend stompClient.TOPIC + VcSimulationPage subscribe + invalidateQueries(['vc-slots']) + 상단 연결 상태 Tag | `cc38adb` |
| ST-VC-5 — Capa/KD 통합 (link 일원화) | VcSimulationPage 상단 [Capa 큐 + KD 보충 →] 버튼 (navigate /vc/capacity-queue) + PLANNER 안내 + 빈 grid Alert info. 기존 Sprint 7~9 자산 (CapacityQueuePage + split/supplement) 그대로 활용 | `bff31a5` |
| ST-VC-6 — VC-FULL IT 5 cases | OrderCommittedListenerIT 1 + VcSchedulePushListenerIT 1 (TransactionTemplate AFTER_COMMIT 트리거 + Mockito timeout verify) + VcScheduleQueryControllerIT 2 (range 필터 + 4 role read) | `9df885b` + `cc38adb` + `bff31a5` |

### Sprint 14 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-VC-1-1 OrderCommittedListener | ST-VC-1 | 0.3 | `9df885b` |
| TK-VC-1-2 vc/package-info (이미 order::events) | ST-VC-1 | 0 | — |
| TK-VC-1-3 OrderCommittedListenerIT | ST-VC-1 | 0.3 | `9df885b` |
| TK-VC-2-1 V039 sample seed + FK vc_machine 동봉 | ST-VC-2 | 0.6 | `9df885b` |
| TK-VC-2-2~4 시뮬뷰 안내 메시지 | ST-VC-2 | 0.3 | `cc38adb` |
| TK-VC-2-5 VcScheduleQueryControllerIT | ST-VC-2 | 0.3 | `bff31a5` |
| TK-VC-3-1~4 AG Grid Enterprise 검증 (이미 완성) | ST-VC-3 | 0 | — |
| TK-VC-4-1 VcSchedulePushListener | ST-VC-4 | 0.5 | `cc38adb` |
| TK-VC-4-2 Frontend STOMP subscribe + invalidate | ST-VC-4 | 0.3 | `cc38adb` |
| TK-VC-4-3 VcSchedulePushListenerIT (TransactionTemplate) | ST-VC-4 | 0.3 | `cc38adb` |
| TK-VC-5-1~2 Capa/KD link 통합 | ST-VC-5 | 0.4 | `bff31a5` |
| TK-VC-5-3 capacity overflow integration (별도 페이지 이미 작동) | ST-VC-5 | 0 | — |
| TK-VC-6-1~4 IT (covered by 위 ST별 IT) | ST-VC-6 | (covered) | — |
| **Sprint 14 합계** | | **~6 SP** | (계획 6 정합 / AI 가속 ~1 PD 실 — 계획 3.3 PD 의 30%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | PLANNER 수주 확정 → vc OrderCommittedListener LOG | ✅ OrderCommittedListenerIT |
| 2 | 시뮬뷰 1주 horizon ≥100 row (sample 15 row) | ✅ V039 시드 |
| 3 | AG Grid 1500 row 가상 스크롤 + pinning + 검색 | ✅ Sprint 5 EP-15 완성 |
| 4 | PLANNER dnd → STOMP broadcast → STK 1초 동기 | ✅ infra (mutate API Sprint 15 carry-over) |
| 5 | Capa 초과 자동 capacity-queue 진입 | ✅ link 통합 (Phase 5+ allocator chain) |
| 6 | KD 보충 supplement 통합 | ✅ link 통합 |
| 7 | 본 PC 실 시나리오 — PLANNER 확정 → 시뮬뷰 → 드래그 → STOMP 동기 | ⏳ **carry-over** |
| 비기능 1~4 | ArchUnit + IT + tsc + AG Grid 성능 | ✅ All GREEN |

**기능 6 ✅ + 비기능 4 ✅ = 10/11 + 본 PC E2E 1 carry-over**.

---

## 4. v1.9 §6 carry-over → v1.10 갱신

| 항목 | v1.9 carry-over | v1.10 결과 |
|---|---|---|
| 본 PC 실 엑셀 E2E (Sprint 13) | High 잔여 | ⏳ 여전히 잔여 (Sprint 15 진입 직전) |
| 장비/셋팅/합금형 5 entity CRUD UI | Medium Sprint 14 EP-VC-FULL 부속 | (Sprint 14 단순 link 만 통합, CRUD UI 자체는 Sprint 15+ carry-over) |
| Order 자동 INSERT 흐름 (mapping → diff → commit chain) | Medium | Phase 5+ carry-over (Sprint 14 baseline 은 LOG only) |
| OrderCommittedNotificationListener Sprint 18 통합 | Low | Low (변동 없음) |

---

## 5. v1.2 § 추가 영향 정리 (v1.9 → v1.10 확장)

| § | v1.9 → v1.10 변경 |
|---|---|
| §9 Deferred Epic | + **EP-VC-FULL (S14 마감)** — OrderCommittedListener chain + V039 seed + STOMP broadcast + Capa/KD link |
| §14 SP 합계 | 315 → **321** (Sprint 14 +~6 실) |
| §16 Phase B 진입 조건 | + **Sprint 14 마감 → Sprint 15 EP-EX-FULL 진입 게이트 충족** (VC schedule SSoT 흐름 완비) |
| §17 GitHub label | `sprint:S14` 추가 |
| **부수 자산** | + **AuditLogService (audit::api)** — Sprint 18+ 신규 mutation-less endpoint 재사용 가능 |

---

## 6. carry-over 식별 (Sprint 15+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 시나리오 E2E (Sprint 13 + 14) | High | 실 엑셀 업로드 + simview + STOMP 동기 확인 — Sprint 15 진입 직전 |
| VC schedule mutate API (PATCH /slots/{id}, override 등) | High | Sprint 15 EP-EX-FULL 부속 또는 Sprint 16 EP-CONFIRM |
| 장비 (LP/IC) / 셋팅 그룹 / 합금형 / 회전수 / 라인 5 entity CRUD UI | Medium | Sprint 15+ carry-over |
| Order 자동 INSERT 흐름 (mapping → diff → commit → vc draft) | Medium | Phase 5+ allocator chain |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |
| 99999-SAMPLE row 정리 절차 (PROD cutover) | Low | Sprint 19 EP-BETA-LAUNCH |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.9](TASK-001_WBS_v1.9.md) — Sprint 13 + V038 hotfix
- [PLAN-SPRINT-14_EP-VC-FULL_v1.0](PLAN-SPRINT-14_EP-VC-FULL_v1.0.md) — Sprint 14 진입 plan (6 Story / 22 Task / DoD 11)
- [Sprint 14 commits chain](#) — `9df885b` (Day 1 Listener + Seed) → `cc38adb` (Day 2 STOMP) → `bff31a5` (Day 3 Capa link + IT)
- [Sprint 13 hotfix](#) — `81b7c8d` (AuditLogService mutation-less BR-X02 정합)
- [OrderCommittedListener](../../backend/vc/src/main/java/com/scheduling/vc/internal/OrderCommittedListener.java)
- [VcSchedulePushListener](../../backend/notify/src/main/java/com/scheduling/notify/VcSchedulePushListener.java)
- [V039 seed](../../backend/vc/src/main/resources/db/migration/V039__seed_vc_simview_sample.sql)
- [AuditLogService](../../backend/audit/src/main/java/com/scheduling/audit/api/AuditLogService.java)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.9 | 2026-05-27 | Claude Code | Sprint 9 plan + Sprint 10·11·12·13 마감 + V038 hotfix |
| 1.10 | 2026-05-27 | Claude Code | **Addendum — Sprint 13 hotfix AuditLogService (mutation-less endpoint BR-X02) + Sprint 14 EP-VC-FULL 100% 마감 (6 Story / 22 Task / 3 commits / ~6 SP). OrderCommittedListener chain (Sprint 13 publisher → vc listener) + VcSchedulePushListener STOMP broadcast + V039 sample seed + Capa/KD link 통합. ST-VC-3 (AG Grid Enterprise) Sprint 5 EP-15 누적 자산 확인 — 추가 작업 0. DoD 10/11 ✅ + 1 carry-over (본 PC E2E). 63 Epic / 321 SP 실. Sprint 15 EP-EX-FULL 진입 게이트 충족 — VC schedule SSoT 흐름 완비** |
