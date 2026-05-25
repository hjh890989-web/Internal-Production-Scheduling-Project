# SRS v1.6 — REQ-FUNC-VC-022·023 Must 승격 (Sprint 7 carry-over 마감 Addendum)

**문서 ID**: SRS-001 | **개정**: 1.6 | **작성일**: 2026-05-23
**전판**: [SRS-001 v1.5](SRS-001_Production_Scheduling_System_v1.5.md) (1906 line, NFR-SEC-007 사번+PIN 정책 재정의)
**상태**: Addendum — Phase 3 (개발) Sprint 7 carry-over 풀 스택 마감 반영

> v1.5 (NFR-SEC-007 사번 8자리 + PIN 4자리) 의 **REQ-FUNC-VC-022·023 Should → Must 승격**. Sprint 7
> carry-over 풀 스택 마감 (백엔드 + UI + REST IT) 완료. 본 문서는 **v1.5 의 변경 델타만 정리** —
> 전체 SRS 콘텐츠는 v1.5 유지.

---

## 1. v1.5 → v1.6 변경 요지

| 항목 | v1.5 (2026-05-19) | v1.6 (2026-05-23) |
|---|---|---|
| REQ-FUNC 총수 | 75 | 75 (변동 없음, 우선순위 승격만) |
| REQ-FUNC-VC-022 우선순위 | Should (활성 후 Must) | **Must** ✅ |
| REQ-FUNC-VC-023 우선순위 | Should (활성 후 Must) | **Must** ✅ |
| Sprint 매핑 | Deferred (Phase B 후) | **Sprint 7 carry-over** ✅ |
| 구현 상태 | TBD | ✅ 백엔드 (V033 + Service 2 + Controller) + UI (api + 2 panel + page + 4 type tests) + REST IT 5 + Service IT 5 = 풀 스택 |
| AC 충족 | (요구) | ✅ BrV12V13IT + CapacityOverflowControllerIT 모두 PASSED |

---

## 2. 승격된 요구사항 상세 (v1.5 §6 갱신)

### REQ-FUNC-VC-022 — capa 초과 시 우선순위 추가 요청 큐 (v1.5 Should → v1.6 **Must**)

| 필드 | v1.6 |
|---|---|
| **출처** | REF-01 §9.3 BR-V12 (Sprint 7 carry-over 마감, deferred 활성 종료) |
| **우선순위** | **Must** (v1.5 — Should 활성 후 Must) |
| **AC** | (1) `Σ Q_required > daily_capa` 시 PRODUCT_PRIORITY rank ASC 기준 분리. (2) 미등록 hose 는 rank 99 fallback. (3) 자동 채택분 + 추가 요청 큐 분리 응답 (Planner UI 미리보기). (4) Planner 1클릭 승인 워크플로우는 **Sprint 8+ carry-over** (별도 Story) |
| **검증** | T-I + T-UAT — `CapacityOverflowQueueServiceTest` (5 unit) + `BrV12V13IT` (Service 3) + `CapacityOverflowControllerIT` (REST 4) — 모두 PASSED |
| **구현** | `vc.capacity_overflow.CapacityOverflowQueueService.split` + REST POST `/api/v1/schedule/vc/capacity-overflow/split` + Frontend `/vc/capacity-queue` Tab1 |
| **활성 조건** | DI-07 PRODUCT_PRIORITY 마스터 입력 (Phase 4-B `SEED_V12V13=1` STG 옵션) |

### REQ-FUNC-VC-023 — capa 부족 시 KD 발주 보충 (v1.5 Should → v1.6 **Must**)

| 필드 | v1.6 |
|---|---|
| **출처** | REF-01 §9.3 BR-V13 (Sprint 7 carry-over 마감) |
| **우선순위** | **Must** (v1.5 — Should 활성 후 Must) |
| **AC** | (1) 동일 hose KD remaining_qty 1차 우선순위. (2) 동일 셋팅 그룹 hose 2차 fallback. (3) `consume(qty, now, actor)` atomic + status 자동 전이 (OPEN→PARTIAL→FILLED). (4) `@Auditable` AOP 로 audit row 자동 발행 (BR-X02). (5) `remaining_qty CHECK ≤ order_qty` invariant 보존 |
| **검증** | T-U + T-I + A — `KdOrderTest` (6 unit) + `BrV12V13IT` (Service 3) + `CapacityOverflowControllerIT` (REST 3, audit principal 검증 포함) — 모두 PASSED |
| **구현** | `vc.capacity_overflow.KdSupplementService.supplement` `@Auditable` + REST POST `/api/v1/schedule/vc/capacity-overflow/supplement` + Frontend `/vc/capacity-queue` Tab2 |
| **활성 조건** | DI-08 KD_ORDER 마스터 입력 (Phase 4-B `SEED_V12V13=1` STG 옵션) |

---

## 3. v1.5 §추적성 매트릭스 갱신

```text
v1.5 매트릭스:
| REQ-FUNC-VC-022 | BR-V12 (v1.4, deferred) | — | TC-VC-022 | — | Should (활성 후 Must) |
| REQ-FUNC-VC-023 | BR-V13 (v1.4, deferred) | — | TC-VC-023 | — | Should (활성 후 Must) |

v1.6 매트릭스:
| REQ-FUNC-VC-022 | BR-V12 (Sprint 7 carry-over 마감) | AC 1-4 | TC-VC-022 = BrV12V13IT + CapacityOverflowControllerIT.split_* | NFR-PER-005 + NFR-SEC-001 (RBAC PLANNER) | **Must** |
| REQ-FUNC-VC-023 | BR-V13 (Sprint 7 carry-over 마감) | AC 1-5 | TC-VC-023 = KdOrderTest + BrV12V13IT + CapacityOverflowControllerIT.supplement_* | NFR-SEC-004 (audit immutable 3년) + NFR-SEC-001 (RBAC PLANNER) | **Must** |
```

---

## 4. v1.5 §"다음 리뷰" 갱신

v1.5 명시 — "다음 리뷰: 수주정보 통합 작업 완료 시점 (REQ-FUNC-VC-022·023 활성 승격), 또는 Phase 1.0 빌드 킥오프 이전".

v1.6 시점 — **양 조건 모두 충족**:
- ✅ 수주정보 통합 작업 완료 (Sprint 1 EP-01)
- ✅ REQ-FUNC-VC-022·023 **활성 승격 본 v1.6 완료**
- ✅ Phase 1.0 빌드 (Sprint 0~7 carry-over) 완료 — Phase 4 진입 게이트 9/9 통과

**다음 리뷰 시점 (v1.7 예상)**: Phase 4-B (베타 시나리오 BS-06 실 운영) 후 — V12 추가 요청 큐 승인 워크플로우 (Sprint 8+) AC 정식화 시점.

---

## 5. 관련 자료

- [SRS v1.5](SRS-001_Production_Scheduling_System_v1.5.md) — 전체 SRS (1906 line, v1.6 변경 외 그대로 유효)
- [TASK-001_WBS_v1.3](../4.Tasks/TASK-001_WBS_v1.3.md) — Sprint 7 carry-over EP-22·23 활성 마감 Addendum
- [ADR-022 Sprint 7 carry-over decisions](../3.SAD/ADR-022_Sprint7_Decisions_v1.0.md) — V033 facade + capacity_overflow 패키지 + @Auditable + RBAC 결정
- [Sprint-7_Completion_v1.1](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md) — Sprint 7 carry-over 6 commit
- [Phase-3_Completion_v1.1](../../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.1.md) — Phase 3 종합 (Sprint 0~7)
- [Phase-4_EntryPlan_v1.1](../../Phase%204/Phase-4_EntryPlan_v1.1.md) — Phase 4 진입 게이트 9/9
- [BS-06](../../docs/operations/beta-scenarios/06-capacity-overflow-kd-supplement.md) — capacity-queue 베타 시나리오

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2025-12~2026-05-15 | (작성자) | SRS 진화 — 75 REQ-FUNC + 60 REQ-NF + 14 SRS-RSK |
| 1.5 | 2026-05-19 | (작성자) | NFR-SEC-007 사번 8자리 + PIN 4자리 + 5회/10분 잠금 (v1.4 12자/3종 폐기) |
| **1.6** | **2026-05-23** | **Claude Code** | **Addendum — REQ-FUNC-VC-022·023 Should → Must 승격. Sprint 7 carry-over 풀 스택 마감 (백엔드 + UI + REST IT). 추적성 매트릭스 갱신. 전체 콘텐츠는 v1.5 유지** |
