# Sprint 8 완료 보고서 v1.0 — V12 승인 워크플로우 + V13 Grafana panel

**Sprint**: S8 (Sprint 7 carry-over 식별 항목 마감) | **기간**: 2026-05-23 (1일) | **상태**: ✓ 완료
**작성**: 2026-05-23 | **상위**: [Sprint-7_Completion_v1.1](Sprint-7_Completion_v1.1.md)

> Sprint 7 carry-over 풀 스택 마감 (v1.1) 시점 차순위로 식별된 **2 항목 본격 진입 + 마감** —
> BR-V12 추가 요청 큐 승인 워크플로우 (코드) + BR-V13 IT_OPS Grafana panel (관측). Phase 4-A 실 STG
> 진입 직전 마지막 코드 작업 마일스톤.

---

## 1. Sprint 8 목표

> "Sprint 7 v1.1 carry-over 차순위 2 Epic 신규 진입 — V12 1클릭 승인/거절 풀 스택 (backend +
>  UI) + V13 IT_OPS KD 잔량 시각화 (metric + Grafana). Phase 4-A 진입 전 완전 마감."

---

## 2. Task 매트릭스 (3 commit, 2 Epic)

### EP-V12-승인 (BR-V12 풀 스택 — Sprint 7 carry-over 식별 마감) — 2 commit

| Task | 상태 | Commit |
|---|---|---|
| V034 `app.capacity_overflow_request` (UUID PK + status PENDING/ACCEPTED/REJECTED + CHECK chk_decided_consistency + chk_reject_reason + transition trigger) | ✓ | `23e9c03` |
| `CapacityOverflowRequest` entity + Repository (status + hose 조회) | ✓ | `23e9c03` |
| `CapacityOverflowApprovalService` — enqueue + accept + reject (priority_rank 보존 + reason 필수 + `@Auditable` × 3) | ✓ | `23e9c03` |
| Controller 확장 — POST /enqueue + /queue/{id}/accept + /queue/{id}/reject (PLANNER `@PreAuthorize`) | ✓ | `23e9c03` |
| Controller GET /queue?status= — PLANNER + IT_OPS + READ_ONLY 광역 조회 | ✓ | `176df99` |
| IT 12 — Service (enqueue + accept + reject + 중복 차단 + 미존재 + V034 trigger) + REST (200 happy × 4 + 403 × 4 + 400 + GET 200 × 2) | ✓ | `23e9c03` + `176df99` |
| Frontend `api/capacityOverflowApi.ts` — 4 신규 method + types (CapacityOverflowRequest + EnqueueResponse) | ✓ | `8961124` |
| Tab1 `CapacityOverflowSplitPanel` 확장 — "큐 등록" 버튼 (split 결과 → enqueue) | ✓ | `8961124` |
| Tab3 `PendingQueuePanel` (신규) — Table + 1클릭 Accept/Reject + reject reason Modal | ✓ | `8961124` |
| `CapacityQueuePage` Tab3 통합 + types test +3 | ✓ | `8961124` |

### EP-V13-Grafana (BR-V13 IT_OPS 시각화) — 1 commit

| Task | 상태 | Commit |
|---|---|---|
| `KdOrderRepository.findRemainingSumByHose()` (group by hose, SUM remaining_qty OPEN+PARTIAL) + projection | ✓ | `8df0df6` |
| `KdOrderLookup.remainingByHose()` facade + Impl (master::api 확장) | ✓ | `8df0df6` |
| vc/build.gradle.kts +micrometer-core 의존성 | ✓ | `8df0df6` |
| `CapacityOverflowMetrics` — MultiGauge × 2 (V13 KD remaining + V12 pending count) + @Scheduled 30s refresh + 예외 swallow | ✓ | `8df0df6` |
| `VcSchedulingEnabledConfig` (notify/order 동일 패턴, @Profile with-infra) | ✓ | `8df0df6` |
| Grafana JSON `capacity-overflow-v12-v13.json` — 4 panel (bargauge + stat + 시계열 × 2 + threshold) | ✓ | `8df0df6` |
| 단위 test 4 — gauge tag 등록 + V12 3 status + 빈 결과 + 예외 swallow | ✓ | `8df0df6` |

**합계** — **3 commit / ~17 신규 파일 / 16 신규 tests (12 IT + 4 unit)**.

---

## 3. 핵심 지표 (Sprint 7 → Sprint 8)

| 영역 | Sprint 7 v1.1 | Sprint 8 |
|---|---:|---:|
| Backend tests | 795 | **811** (+16) |
| Frontend vitest | 58 | **61** (+3 types) |
| Flyway 마이그레이션 | V001~V033 (34) | **V001~V034** (35) — `app.capacity_overflow_request` + transition trigger |
| Frontend chunk `CapacityQueuePage` gzip | 2.72kB | **3.85kB** (+1.13kB Tab3 + Modal) |
| Vite entry first paint gzip | 7.23kB | 7.23kB (변동 없음) |
| Grafana dashboards | 7 | **8** (+ capacity-overflow-v12-v13) |
| Prometheus metrics 신규 | — | `scheduling.v13.kd.remaining.qty{hose}` + `scheduling.v12.pending.request.count{status}` |
| Modulith verify | 0 위반 | 0 위반 |
| ArchUnit | 29 rule | 29 rule |

---

## 4. 핵심 deliverable — V12 풀 스택 + V13 관측

### EP-V12 워크플로우 (전후 비교)

| 단계 | Sprint 7 v1.1 | Sprint 8 |
|---|---|---|
| 1. Split 미리보기 | ✅ Tab1 | ✅ Tab1 (그대로) |
| 2. requestQueue 등록 | 미리보기만 (영속 0) | ✅ Tab1 "큐 등록" 버튼 → POST /enqueue → DB PENDING |
| 3. Pending 조회 | 화면 없음 | ✅ Tab3 PendingQueuePanel → GET /queue?status=PENDING (priority_rank ASC) |
| 4. Planner 결정 | 화면 없음 | ✅ 1클릭 승인 + reason modal 거절 → POST /accept\|reject |
| 5. audit | 자동 (@Auditable supplement 만) | ✅ enqueue + accept + reject 3 method 모두 @Auditable |
| 6. 중복 결정 차단 | — | ✅ Domain IllegalStateException + V034 DB trigger 이중 보장 |

### EP-V13 관측 (IT_OPS 진입점)

```text
Prometheus scrape (15s) → /actuator/prometheus
  ↓ scheduling.v13.kd.remaining.qty{hose=...}  (MultiGauge × N hose)
  ↓ scheduling.v12.pending.request.count{status=PENDING|ACCEPTED|REJECTED}
Grafana dashboard "capacity-overflow-v12-v13" (4 panel)
  ↓ IT_OPS 즉시 진입 — http://grafana.intranet:3000
```

활성 조건 — DI-07/08 마스터 입력 후 비-zero (미입력 시 그래프 0).

---

## 5. 3 Commit 시간순

```text
23e9c03  feat(vc): Sprint 8 BR-V12 추가 요청 큐 승인 워크플로우 backend (REQ-FUNC-VC-022)
176df99  feat(vc): Sprint 8 V12 GET /queue?status= endpoint + 2 IT
8961124  feat(ui): Sprint 8 V12 UI 확장 — Tab1 큐 등록 + Tab3 PendingQueuePanel (1클릭 승인/거절)
8df0df6  feat(observability): Sprint 8 EP-V13-Grafana — IT_OPS KD remaining + V12 pending queue metric + dashboard
```

(4 commit — 위 표는 Epic 2 기준 3 라인으로 간소화. 실 commit 4개.)

---

## 6. Sprint 8 Velocity

- **계획**: WBS v1.3 Carry-over §11 — V12 승인 워크플로우 Medium + V13 Grafana panel Medium (정확한 SP 산정 안 함)
- **실제**: ~6 SP / 0.5일 (AI 가속 vibe coding)
- **누적 commit (Sprint 0~8)**: ~194

---

## 7. Phase 4-A 진입 자산 — Sprint 8 추가 효과

본 Sprint 8 마감 후 [Phase-4_EntryPlan_v1.1](../../Phase%204/Phase-4_EntryPlan_v1.1.md) 진입 게이트 9/9
유지 + 운영 자산 추가:
- BS-06 (Phase 4-B 후반) — V12 워크플로우 풀 스택 검증 가능 (split → enqueue → accept|reject + Grafana 시각화)
- IT_OPS Grafana — KD 잔량 사전 경고 + V12 큐 누적 모니터링

---

## 8. 차순위 carry-over (Phase 5+ / Sprint 9+)

| 항목 | 분류 | 우선 |
|---|---|---|
| ~~BR-V12 승인 워크플로우~~ | ✓ done | Sprint 8 마감 |
| ~~BR-V13 Grafana panel~~ | ✓ done | Sprint 8 마감 |
| V12 ACCEPTED 시 vc_schedule 자동 INSERT chain (Allocator 후속 처리) | 운영 | Sprint 9+ (실 베타 운영 후 자연) |
| V12 PENDING 자동 만료 (예: 24h 후 자동 REJECTED) | 운영 | Sprint 9+ |
| Mobile App (Flutter 압출 패드) | UX | Phase 5+ |
| ML 추천 (EP-18 ranking 자동화) | AI | Phase 6+ |
| ArchUnit DDD layer 강화 | 품질 | Medium |
| 사내 NAS S3 호환 (Excel attachment) | 인프라 | Phase 5+ |

---

## 9. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 8 V12 승인 워크플로우 풀 스택 + V13 Grafana panel 마감 (4 commit, ~17 신규 파일, 16 신규 tests, V034 + 2 metric + 4 Grafana panel) |
