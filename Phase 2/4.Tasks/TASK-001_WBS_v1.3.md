# 작업 분할 구조서 (WBS) v1.3 — Sprint 7 carry-over 반영 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.3 | **작성일**: 2026-05-23
**전판**: [TASK-001_WBS_v1.2.md](TASK-001_WBS_v1.2.md) (1303 line, Phase 2 마지막 설계 산출물)
**상태**: Addendum — Phase 3 (개발) Sprint 0~6 + **Sprint 7 carry-over 풀 스택 마감** 반영

> v1.2 (Phase 2 종료 시점 285 SP / 49 Epic 설계) 의 **deferred 항목 EP-22·23 활성 마감** + Phase 4 진입
> 게이트 충족. 본 문서는 **v1.2 의 변경 델타만 정리** — 전체 WBS 콘텐츠는 v1.2 유지.

---

## 1. v1.2 → v1.3 변경 요지

| 항목 | v1.2 (Phase 2 종료) | v1.3 (Phase 3 종료 + Sprint 7 carry-over) |
|---|---|---|
| Epic 총수 | 49 (47 active + 2 deferred) | **49** (49 active — EP-22·23 deferred 마감) |
| SP 총수 | 285 (275 Phase 1.0 + 10 Deferred) | **285** (Deferred 10 SP 마감 — 실 ~5 SP / 0.5일 vibe 가속) |
| 상태 | Phase 3 진입 대기 | **Phase 4 진입 대기** (게이트 9/9) |
| Deferred 항목 | EP-22 + EP-23 (Phase B 후) | ✅ **Sprint 7 carry-over 백엔드 + UI + REST IT 풀 스택 마감** (활성 조건 — DI-07/08 입력만 대기) |

---

## 2. Sprint 7 carry-over Epic 활성 상세 (v1.2 §9 갱신)

### EP-22 (v1.2 Deferred → **v1.3 ✅ done**) capa 초과 시 우선순위 추가요청 큐

**Sprint**: **S7 carry-over** (2026-05-23) / **SP 계획**: 5 / **SP 실**: ~2.5 (AI 가속)

| Story | 구현 | 상태 |
|---|---|---|
| ST-22-1 PRODUCT_PRIORITY 마스터 | V033 master.product_priority (hose_id PK + priority_rank 1-99 CHECK + effective_from/to) + ProductPriority entity + ProductPriorityRepository + master.api.ProductPriorityLookup facade | ✅ commit `9a65847` |
| ST-22-2 추가요청 큐 분기 로직 + 사용자 게이트 | CapacityOverflowQueueService.split (priority rank ASC + rank 99 fallback + accepted/requestQueue 분리) + 단위 5 (CapacityOverflowQueueServiceTest) + IT (BrV12V13IT) + **REST POST /split** + **Frontend Tab1 미리보기** + REST IT 3 (PLANNER 200 + STK_USER 403 + 미인증 401 + dailyCapa @Min 400) | ✅ commit `d1610a7` + `1f1313f` + `9f3f5f0` + `1671157` + `c3ffeea` |

> v1.2 명시 — "사용자 게이트 (Sprint 8+ 승인 confirm UI)" 은 미리보기만 마감. 실 1클릭 승인 워크플로우는 **Sprint 8+ carry-over** (Phase 6 로드맵).

### EP-23 (v1.2 Deferred → **v1.3 ✅ done**) capa 부족 시 KD 발주 보충

**Sprint**: **S7 carry-over** (2026-05-23) / **SP 계획**: 5 / **SP 실**: ~2.5 (AI 가속)

| Story | 구현 | 상태 |
|---|---|---|
| ST-23-1 KD_ORDER 마스터 + 캐시 | V033 master.kd_order (UUID PK + remaining_qty CHECK ≤ order_qty + 4-status 머신) + KdOrder entity + consume(qty, now, actor) + 단위 6 + master.api.KdOrderLookup facade | ✅ commit `9a65847` |
| ST-23-2 KD 보충 우선순위 (동일품번 → 셋팅그룹) | KdSupplementService.supplement @Auditable (동일 hose 1차 + 셋팅 그룹 2차 fallback + atomic consume + status 자동 전이) + IT (BrV12V13IT 3) + **REST POST /supplement** + **Frontend Tab2 1클릭 보충** + REST IT 2 (PLANNER 200 + READ_ONLY 403 + shortage @Min 400) | ✅ commit `d1610a7` + `1f1313f` + `9f3f5f0` + `1671157` + `c3ffeea` |

---

## 3. Phase 4 진입 게이트 (v1.2 §16 Phase B 조건 갱신)

v1.2 §16 — "Phase B (수주정보 통합) 진입 조건 — Deferred Activation":

| v1.2 조건 | v1.3 상태 |
|---|---|
| 수주통합 PDD-01 안정화 (Sprint 1 EP-01) | ✅ done (Sprint 1) |
| SRS v1.4 REQ-FUNC-VC-022·023 활성 승격 | ✅ Sprint 7 carry-over — 백엔드 + UI + REST IT 풀 스택 |
| DI-07 PRODUCT_PRIORITY 마스터 입력 | ⏸ Phase 4-B 후반 (STG `SEED_V12V13=1` sample seed 옵션 추가) |
| DI-08 KD_ORDER 마스터 입력 | ⏸ Phase 4-B 후반 (동) |

→ **Phase 4 진입 시점에 코드 자산 100% 완비, 운영 데이터 입력만 대기**.

---

## 4. Sprint 7 carry-over 신규 Task 매트릭스 (v1.2 §6 보완)

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-22-1-1 master.product_priority DDL + entity + facade | ST-22-1 | 0.5 | `9a65847` |
| TK-22-2-1 CapacityOverflowQueueService split | ST-22-2 | 1 | `d1610a7` |
| TK-22-2-3 BrV12V13IT (Service 5) + audit 검증 | ST-22-2 | 0.3 | `d1610a7` |
| **🆕 TK-22-2-4 REST controller + Frontend UI Tab1 + REST IT 3** | ST-22-2 | 0.7 | `1f1313f` + `9f3f5f0` + `1671157` |
| TK-23-1-1 master.kd_order DDL + entity + consume() + facade | ST-23-1 | 0.5 | `9a65847` |
| TK-23-2-1 KdSupplementService (동일 hose + 그룹 fallback) | ST-23-2 | 1 | `d1610a7` |
| TK-23-2-3 BrV12V13IT 추가 (KD 3 케이스) | ST-23-2 | 0.3 | `d1610a7` |
| **🆕 TK-23-2-4 REST controller + Frontend UI Tab2 + REST IT 2** | ST-23-2 | 0.7 | `1f1313f` + `9f3f5f0` + `1671157` |
| **🆕 TK-22·23-5 @Valid quality fix + 400 IT 2** | ST-22-2 + ST-23-2 | 0.2 | `c3ffeea` |
| 합계 | EP-22 + EP-23 | **~5 SP** | (vs 계획 10 SP — AI 가속 2배 압축) |

---

## 5. v1.2 § 추가 영향 정리

| § | v1.2 → v1.3 변경 |
|---|---|
| §9 Deferred Epic | EP-22·23 → 활성 마감 (Sprint 7 carry-over), Phase 6 carry-over 로 **Sprint 8+** 항목 신규 — V12 승인 워크플로우 (UI commit/reject + endpoint + audit) + V13 Grafana panel (IT_OPS KD remaining_qty) |
| §14 SP 합계 | Deferred 10 SP → 실 ~5 SP (AI 가속), Phase 1.0 + Deferred = **285 SP 마감** |
| §16 Phase B 진입 조건 | 수주통합 + REQ-FUNC-VC-022·023 활성 ✅, **DI-07/08 sample seed STG 옵션 (`SEED_V12V13=1`) 추가** |
| §17 GitHub label | `sprint:Deferred` → `sprint:S7-carry-over` (label rename 권장, Phase 4 진입 후 cleanup) |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.2.md](TASK-001_WBS_v1.2.md) — 전체 WBS (1303 line, v1.3 변경 외 그대로 유효)
- [Sprint-7_Completion_v1.1.md](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md) — Sprint 7 carry-over 풀 스택 마감 6 commit
- [Phase-3_Completion_v1.1.md](../../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.1.md) — Phase 3 종합 (Sprint 0~7)
- [Phase-4_EntryPlan_v1.1.md](../../Phase%204/Phase-4_EntryPlan_v1.1.md) — Phase 4 진입 게이트 9/9
- [BS-06](../../docs/operations/beta-scenarios/06-capacity-overflow-kd-supplement.md) — capacity-queue 베타 시나리오 (DI-07/08 활성 조건)
- 4 페르소나 v1.1 ([Planner](../../docs/operations/persona/01-planner.md) / [STK_USER](../../docs/operations/persona/02-stk-user.md) / [IT_OPS](../../docs/operations/persona/03-it-ops.md) / [READ_ONLY](../../docs/operations/persona/04-read-only.md))

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — REF-PDD v1.4 + REF-SRS v1.4 + REF-SAD v1.1 Epic·Story·Task 3단계 분해 |
| 1.1 | 2026-05-15 | (작성자) | 산술 오류 정정 + EP-34 누락 보강 |
| 1.2 | 2026-05-15 | (작성자) | v1.1 5개 기준 부합성 결함 10건 (REV-D-001~010) 전면 해소 — Phase 3 진입 게이트 통과 (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | **Addendum** — Sprint 7 carry-over EP-22·23 deferred 활성 마감 (V033 + 백엔드 + UI + REST IT). Phase 4 진입 게이트 9/9 충족. 전체 WBS 콘텐츠는 v1.2 유지, 본 v1.3 은 델타만 정리 |
