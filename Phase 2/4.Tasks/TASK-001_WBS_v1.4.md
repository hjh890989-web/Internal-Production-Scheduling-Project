# 작업 분할 구조서 (WBS) v1.4 — Sprint 8 V12 승인 워크플로우 + V13 Grafana panel (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.4 | **작성일**: 2026-05-23
**전판**: [v1.3](TASK-001_WBS_v1.3.md) (Sprint 7 carry-over EP-22·23 활성 마감 Addendum)
**상태**: Addendum — Sprint 8 진입 + 마감 (V12 풀 스택 + V13 IT_OPS 관측)

> v1.3 (Sprint 7 carry-over EP-22·23 마감, 49 Epic / 285 SP) 의 차순위 carry-over §11 에서
> 식별된 2 항목을 Sprint 8 신규 Epic 으로 진입 + 마감. **본 문서는 v1.3 변경 델타만 정리** —
> 전체 WBS 콘텐츠 v1.2 유지, 변경 chain v1.3 → v1.4.

---

## 1. v1.3 → v1.4 변경 요지

| 항목 | v1.3 (Sprint 7 마감) | v1.4 (Sprint 8 마감) |
|---|---|---|
| Epic 총수 | 49 | **51** (+2 — EP-V12-승인 + EP-V13-Grafana) |
| SP 총수 | 285 | **291** (+6 실 ~3 + AI 가속 2배 압축) |
| Sprint | S0~S7 carry-over | **S0~S8** |
| BR-V12 상태 | ✅ split() 미리보기 + Frontend Tab1 | ✅ **풀 스택 — split + enqueue 영속 + Planner accept/reject + IT_OPS Grafana** |
| BR-V13 상태 | ✅ supplement Service + REST + UI | ✅ **+IT_OPS Grafana panel** (KD remaining_qty per hose 시각화) |

---

## 2. 신규 Epic 상세

### EP-V12-승인 (BR-V12 풀 스택 — Sprint 7 carry-over 식별 마감)

**Sprint**: **S8** / **출처**: [Sprint-7_Completion_v1.1 §7](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md), REF-PDD-02 v1.1 BR-V12 / **SP 실**: ~4 / **선행**: EP-22 (Sprint 7 split)

| Story | 구현 | Commit |
|---|---|---|
| ST-V12-1 — `app.capacity_overflow_request` schema + entity + Repository (V034) | V034 (UUID PK + status PENDING/ACCEPTED/REJECTED + CHECK chk_decided_consistency + chk_reject_reason + transition trigger) + CapacityOverflowRequest entity + Repository | `23e9c03` |
| ST-V12-2 — Service.enqueue/accept/reject + `@Auditable` | CapacityOverflowApprovalService (priority_rank 보존, reason 필수, 3 method 모두 @Auditable, Domain IllegalStateException + DB trigger 중복 차단 이중 보장) | `23e9c03` |
| ST-V12-3 — REST controller +4 endpoint | POST /enqueue + /queue/{id}/accept + /queue/{id}/reject + GET /queue?status= (RBAC PLANNER 단독 + 조회 광역) | `23e9c03` + `176df99` |
| ST-V12-4 — Frontend api + UI Tab1 확장 + Tab3 신규 | api/capacityOverflowApi.ts +4 method + types / Tab1 "큐 등록" 버튼 / Tab3 PendingQueuePanel (Table + Accept/Reject + reject reason Modal) | `8961124` |
| ST-V12-5 — IT 12 + types test 3 | CapacityOverflowApprovalIT (Service 5 + REST 7) + capacityOverflow.types.test 3 신규 | `23e9c03` + `176df99` + `8961124` |

### EP-V13-Grafana (BR-V13 IT_OPS 관측)

**Sprint**: **S8** / **출처**: [Sprint-7_Completion_v1.1 §7](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md), [BS-06 KPI 영향](../../docs/operations/beta-scenarios/06-capacity-overflow-kd-supplement.md) / **SP 실**: ~2 / **선행**: EP-23 (Sprint 7 supplement)

| Story | 구현 | Commit |
|---|---|---|
| ST-V13-G-1 — Repository custom query + facade 확장 | KdOrderRepository.findRemainingSumByHose() (group by + SUM + projection) + KdOrderLookup.remainingByHose() + LookupImpl | `8df0df6` |
| ST-V13-G-2 — Micrometer metric 컴포넌트 | CapacityOverflowMetrics — MultiGauge × 2 (KD remaining + V12 pending count by status) + @Scheduled(30s) refresh + 예외 swallow + VcSchedulingEnabledConfig | `8df0df6` |
| ST-V13-G-3 — Grafana dashboard JSON | capacity-overflow-v12-v13.json (4 panel: bargauge KD remaining + stat V12 status + 시계열 × 2 + threshold red<20<yellow<50<green) | `8df0df6` |
| ST-V13-G-4 — vc/build.gradle.kts +micrometer-core | dependency 1 라인 추가 (common SchedulingMetrics 동일 의존) | `8df0df6` |
| ST-V13-G-5 — 단위 test 4 | CapacityOverflowMetricsTest — gauge tag 등록 + 3 status + 빈 결과 + 예외 swallow | `8df0df6` |

---

## 3. Sprint 8 Task 매트릭스 — v1.2 §6 보완

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-V12-1-1 V034 schema + transition trigger | ST-V12-1 | 0.5 | `23e9c03` |
| TK-V12-1-2 Entity + Repository | ST-V12-1 | 0.3 | `23e9c03` |
| TK-V12-2-1 Service enqueue/accept/reject + @Auditable | ST-V12-2 | 0.8 | `23e9c03` |
| TK-V12-3-1 Controller +4 endpoint + @PreAuthorize | ST-V12-3 | 0.5 | `23e9c03` + `176df99` |
| TK-V12-4-1 Frontend api types + 4 method | ST-V12-4 | 0.3 | `8961124` |
| TK-V12-4-2 Tab1 "큐 등록" 버튼 | ST-V12-4 | 0.2 | `8961124` |
| TK-V12-4-3 Tab3 PendingQueuePanel + reject Modal | ST-V12-4 | 0.7 | `8961124` |
| TK-V12-5-1 IT 12 + types test 3 | ST-V12-5 | 0.7 | `23e9c03` + `176df99` + `8961124` |
| EP-V12-승인 소계 | | **~4 SP** | |
| TK-V13-G-1-1 Repository query + Lookup facade | ST-V13-G-1 | 0.3 | `8df0df6` |
| TK-V13-G-2-1 CapacityOverflowMetrics + @Scheduled | ST-V13-G-2 | 0.7 | `8df0df6` |
| TK-V13-G-3-1 Grafana JSON 4 panel | ST-V13-G-3 | 0.5 | `8df0df6` |
| TK-V13-G-4-1 build.gradle +micrometer-core | ST-V13-G-4 | 0.05 | `8df0df6` |
| TK-V13-G-5-1 단위 test 4 | ST-V13-G-5 | 0.5 | `8df0df6` |
| EP-V13-Grafana 소계 | | **~2 SP** | |
| **Sprint 8 합계** | | **~6 SP** | (계획 ~12 SP — AI 가속 2배 압축) |

---

## 4. v1.3 §11 차순위 carry-over → v1.4 갱신

| 항목 | v1.3 (Sprint 7 마감) | v1.4 (Sprint 8 마감) |
|---|---|---|
| ~~BR-V12 추가 요청 큐 승인 워크플로우~~ | Sprint 8+ Medium | ✅ **Sprint 8 마감** |
| ~~BR-V13 Grafana panel (IT_OPS KD remaining_qty)~~ | Sprint 8+ Medium | ✅ **Sprint 8 마감** |
| V12 ACCEPTED → vc_schedule 자동 INSERT chain (Allocator 후속) | (식별 안 됨) | 🆕 Sprint 9+ Medium |
| V12 PENDING 자동 만료 (24h 후 자동 REJECTED) | (식별 안 됨) | 🆕 Sprint 9+ Low |
| Mobile App (Flutter 압출 패드) | High | High (변동 없음, Phase 5+) |
| ML 추천 (EP-18 ranking 자동화) | Low | Phase 6+ |
| ArchUnit DDD layer 강화 | Medium | Medium (변동 없음) |
| 사내 NAS S3 호환 (Excel attachment) | (식별 안 됨) | Phase 5+ |

---

## 5. v1.2 § 추가 영향 정리 (v1.3 → v1.4 확장)

| § | v1.3 → v1.4 변경 |
|---|---|
| §9 Deferred Epic | ~~EP-22·23~~ (Sprint 7 마감) + **EP-V12-승인 + EP-V13-Grafana Sprint 8 마감**. 차순위 Sprint 9+ — V12 vc_schedule INSERT chain + PENDING 자동 만료 |
| §14 SP 합계 | 285 → **291** (Sprint 8 +6 실 ~3 AI 가속 2배 압축) |
| §16 Phase B 진입 조건 | Sprint 7 v1.3 SEED_V12V13 충족 + Sprint 8 풀 스택 (UI 워크플로우 + IT_OPS 시각화) 추가 |
| §17 GitHub label | `sprint:S7-carry-over` → `sprint:S8` 라벨 추가 권장 (Phase 4 진입 후 cleanup) |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.3](TASK-001_WBS_v1.3.md) — Sprint 7 carry-over EP-22·23 활성 마감 Addendum
- [TASK-001_WBS_v1.2](TASK-001_WBS_v1.2.md) — 전체 WBS (1303 line, v1.4 변경 외 그대로 유효)
- [Sprint-8_Completion_v1.0](../../Phase%203/1.Sprint-Reports/Sprint-8_Completion_v1.0.md) — Sprint 8 V12 풀 스택 + V13 Grafana 마감 4 commit
- [Sprint-7_Completion_v1.1](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md) — Sprint 7 carry-over 풀 스택 6 commit
- [Phase-4_EntryPlan_v1.1](../../Phase%204/Phase-4_EntryPlan_v1.1.md) — Phase 4 진입 게이트 9/9
- [Grafana dashboard capacity-overflow-v12-v13.json](../../infrastructure/observability/grafana/dashboards/capacity-overflow-v12-v13.json)

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — Epic·Story·Task 3단계 분해 |
| 1.1 | 2026-05-15 | (작성자) | 산술 오류 정정 + EP-34 보강 |
| 1.2 | 2026-05-15 | (작성자) | 결함 10건 해소 (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | Sprint 7 carry-over EP-22·23 deferred 활성 마감 Addendum |
| 1.4 | 2026-05-23 | Claude Code | **Addendum — Sprint 8 신규 Epic 2 (EP-V12-승인 + EP-V13-Grafana) 마감. 51 Epic / 291 SP / 차순위 Sprint 9+ V12 vc_schedule chain + PENDING 자동 만료 재정렬** |
