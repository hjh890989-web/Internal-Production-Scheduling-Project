# 작업 분할 구조서 (WBS) v1.9 — Sprint 13 EP-OC-FULL 마감 + V038 hotfix (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.9 | **작성일**: 2026-05-27
**전판**: [v1.8](TASK-001_WBS_v1.8.md) (Sprint 12 EP-MASTER-UI 마감 Addendum)
**상태**: Addendum — Sprint 13 EP-OC-FULL (수주통합 PDD-01 완성) 100% 마감 + DoD 9/10 ✅ + 1 carry-over (본 PC 실 엑셀 E2E). V038 hotfix (Sprint 7 V033 audit trigger 누락 closure) 포함.

> v1.8 (Sprint 12 EP-MASTER-UI 마감, 63 Epic / 310 SP 실) 의 본 PC 시각 검증 중 식별된
> **product_priority + kd_order audit trigger 누락 (V033 Sprint 7 hotfix)** 을 V038 로 closure.
> Sprint 13 ST-OC-1~6 6 Story 신규 진입 + 마감 — Sprint 1~3 누적 자산 활용 + 핵심 gap 보강.

---

## 1. v1.8 → v1.9 변경 요지

| 항목 | v1.8 (Sprint 12 마감) | v1.9 (Sprint 13 + V038 마감) |
|---|---|---|
| Epic 총수 | 63 | 63 (변동 없음, EP-OC-FULL 마감만) |
| SP 실 합 | 310 | **315** (+~5 실, Sprint 13 계획 5 정합) |
| Sprint 13 상태 | 계획 5 SP | ✅ **마감** (6 Story / 23 Task / 3 commits / ~1 PD AI 가속) |
| Diff UI | 미존재 (OrderImportPage `onProceed` stub) | ✅ **DiffPage.tsx + Commit UI 완성** (severity 카드 + CRITICAL 빨강 + PLANNER 확정/거절 Modal) |
| Diff/Commit API | 미존재 (자산만 존재) | ✅ **DiffController + OrderCommitController + OrderCommittedEvent** |
| audit trigger 커버리지 | product_priority/kd_order 누락 | ✅ **V038 hotfix — BR-X02 완전 충족** (Sprint 7 V033 누락 closure) |
| Sprint 14 진입점 | OrderCommittedEvent publisher 부재 | ✅ **publisher 완성** (vc 모듈 listener 가 성형 스케줄 진입점) |

---

## 2. Sprint 13 마감 — EP-OC-FULL 6 Story 회고

### EP-OC-FULL 전체 (수주통합 PDD-01 완성)

**Sprint**: **S13** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-13_EP-OC-FULL_v1.0](PLAN-SPRINT-13_EP-OC-FULL_v1.0.md) (3-Day) / **SP 실**: ~5 / **선행**: EP-MASTER-UI (S12)

| Story | 구현 | Commit |
|---|---|---|
| ST-OC-1 — DiffController + DiffSummaryResponse | GET /api/v1/orders/{trackingId}/diff (severity count CRITICAL/IMPORTANT/STANDARD/UNCLASSIFIED + RowDiffSummary list) + RBAC PLANNER+IT_OPS+READ_ONLY + 빈 trackingId 200 empty | `c39354b` |
| ST-OC-2 — OrderCommitController + Event | POST commit (PLANNER, @Auditable BR-X02, reason 필수, OrderCommittedEvent publisher) + POST reject (audit only, event 미발행) + 미존재 404 + OrderCommittedEvent record (events 패키지) | `c39354b` |
| ST-OC-3 — DiffPage.tsx + orderDiffApi | severity 통계 카드 4 (Statistic + valueStyle 색상) + Ant Table (diffType tag + Hose + 납기 + fieldDiffs ellipsis + 버전) + CRITICAL row .diff-row-critical 빨강 highlight + 상단 정렬 + orderDiffApi 3 method | `2383a2d` |
| ST-OC-4 — Commit UI 통합 | [확정] [거절] 버튼 + useAuthStore.hasRole('PLANNER') 가드 + reason 필수 Modal + 403/404 메시지 분기 + 성공 토스트 + /home navigate + OrderImportPage.onProceed navigate 통합 + router /orders/diff/:trackingId | `2383a2d` |
| ST-OC-5 — Notification stub (Sprint 1~3 기존 자산 + 신규 listener) | 기존 OrderDiffPersistedListener + NotificationService (in-app + 카카오 라우팅 + NotificationEntity 영속 + DEV fallback) 완성 확인. 신규 OrderCommittedNotificationListener (Sprint 18 EP-NOTIFY 진입점, LOG only) | `8d7c545` |
| ST-OC-6 — OC-FULL IT 6 cases | OrderDiffAndCommitIT 5 cases (Diff 2 + Commit 3 — PLANNER 200 + Event 발행 Awaitility / STK 403 / 미존재 404) + Frontend orderDiff 4 unit tests | `c39354b` + `2383a2d` |
| 부수 — order 모듈 audit::aop 의존성 | order/build.gradle.kts `implementation(project(":audit"))` + package-info.java `allowedDependencies += "audit::aop"` | `c39354b` |
| 부수 — Frontend client 204 처리 | client.ts 204/Content-Length=0/빈 text 처리 (Sprint 12 hotfix 재사용) | (Sprint 12 commit) |

### Sprint 13 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-OC-1-1~3 DiffController + DTO + read API | ST-OC-1 | 0.5 | `c39354b` |
| TK-OC-2-1 OrderCommitController commit/reject | ST-OC-2 | 0.3 | `c39354b` |
| TK-OC-2-2 OrderCommittedEvent + publisher | ST-OC-2 | 0.3 | `c39354b` |
| TK-OC-2-3 DecisionPayload + reject reason 필수 | ST-OC-2 | 0.1 | `c39354b` |
| TK-OC-3-1~2 DiffPage severity 카드 + Table + CRITICAL highlight | ST-OC-3 | 1.0 | `2383a2d` |
| TK-OC-3-3 orderDiffApi.ts | ST-OC-3 | 0.2 | `2383a2d` |
| TK-OC-3-4~5 OrderImportPage navigate + router | ST-OC-3 | 0.3 | `2383a2d` |
| TK-OC-4-1~3 Commit UI 확정/거절 버튼 + reason Modal + role 가드 | ST-OC-4 | 0.7 | `2383a2d` |
| TK-OC-4-4 useAuthStore.hasRole 통합 | ST-OC-4 | 0.1 | `2383a2d` |
| TK-OC-4-5 orderDiff 4 unit tests | ST-OC-4 | 0.2 | `2383a2d` |
| TK-OC-5-1 NotificationStubService (기존 자산 활용 — 작업 0) | ST-OC-5 | 0.1 | (기존) |
| TK-OC-5-2 OrderCommittedNotificationListener (LOG stub) | ST-OC-5 | 0.3 | `8d7c545` |
| TK-OC-6-1~2 Diff/Commit IT 5 cases | ST-OC-6 | 0.6 | `c39354b` |
| TK-OC-6-3 Notification listener IT (기존 OrderDiff covers) | ST-OC-6 | (covered) | — |
| 부수 — order 모듈 audit::aop 의존성 | (보충) | 0.3 | `c39354b` |
| **Sprint 13 합계** | | **~5 SP** | (계획 5 정합 / AI 가속 ~1 PD 실 — 계획 2.7 PD 의 37%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | Excel 업로드 → 매핑 → diff 자동 진입 | ✅ (OrderImportPage.onProceed → navigate) |
| 2 | DiffPage severity 카드 4종 (전체/CRITICAL/IMPORTANT/STANDARD) | ✅ |
| 3 | CRITICAL row 빨간 배경 + 상단 정렬 | ✅ |
| 4 | PLANNER 확정 → 200 + audit_log.actor=사번 | ✅ OrderDiffAndCommitIT (decidedBy='00000001') |
| 5 | STK_USER/READ_ONLY read only (확정 비활성) | ✅ (hasRole 가드 + STK 403 IT) |
| 6 | critical count > 0 시 in-app 알림 | ✅ (기존 OrderDiffPersistedListener — Sprint 1~3) |
| 7 | 본 PC 실 엑셀 E2E | ⏳ **carry-over** (Sprint 14 진입 직전) |
| 비기능 1~3 | ArchUnit (PreAuthorize + Modulith) + IT + tsc | ✅ All GREEN |

**기능 6 ✅ + 비기능 3 ✅ = 9/10 + 본 PC E2E carry-over 1**.

---

## 3. V038 hotfix — Sprint 7 V033 audit trigger 누락 closure

| 항목 | v1.8 | v1.9 |
|---|---|---|
| master.product_priority audit | ❌ 누락 (V033 신설 시 trigger 동봉 안 함) | ✅ **V038 trigger 추가** |
| master.kd_order audit | ❌ 누락 | ✅ **V038 trigger 추가** |
| BR-X02 mutation audit 커버리지 | 부분 (master 2 entity 미커버) | ✅ **완전** (모든 mutation entity audit 기록) |
| 발견 시점 | Sprint 12 ST-MASTER-3·4 사용자 직접 검증 | (식별) |

**V025 패턴 + V035 (Sprint 9 hotfix) 정합** — schema 신설 시 audit trigger 동봉 표준 재확인.

| Migration | Schema | Audit trigger 동봉 |
|---|---|---|
| V025 | vc_schedule + ex_candidate + order | ✅ 같이 |
| V033 (S7) | product_priority + kd_order | ❌ → **V038 (S12+13 hotfix) closure** |
| V034 (S8) | capacity_overflow_request | ❌ → V035 (S9 hotfix) |
| V036 (S10) | user_account | ✅ 같이 (V035 교훈) |

---

## 4. v1.8 §5 carry-over → v1.9 갱신

| 항목 | v1.8 carry-over | v1.9 결과 |
|---|---|---|
| 본 PC 시각 검증 (Hub + 4 카드) | High Sprint 12 잔여 | ✅ **사용자 직접 검증 완료** (audit_log.actor=00000007 + BR-V12 즉시 반영 확인) |
| ~~product_priority + kd_order audit trigger 누락~~ | (식별 안 됨) | ✅ **V038 hotfix 마감** |
| 장비/셋팅/합금형 5 entity CRUD UI | Medium Sprint 14 EP-VC-FULL 부속 | Medium (변동 없음) |
| ProductSpec CRUD UI | Medium Sprint 13 EP-OC-FULL | (Sprint 13 은 read 만 — CRUD 는 Sprint 14 carry-over) |

---

## 5. v1.2 § 추가 영향 정리 (v1.8 → v1.9 확장)

| § | v1.8 → v1.9 변경 |
|---|---|
| §9 Deferred Epic | + **EP-OC-FULL (S13 마감)** — Diff/Commit endpoint + UI + Sprint 14 EP-VC-FULL publisher 진입점 |
| §14 SP 합계 | 310 → **315** (Sprint 13 +~5 실) |
| §16 Phase B 진입 조건 | + **Sprint 13 EP-OC-FULL 마감 → Sprint 14 EP-VC-FULL 진입 게이트 충족** (Order SSoT 확정 가능) |
| §17 GitHub label | `sprint:S13` 추가 |

---

## 6. carry-over 식별 (Sprint 14+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 엑셀 E2E (DoD #7) | High (Sprint 13 잔여) | 실 .xlsx 업로드 → 파싱 → diff → 확정 — Sprint 14 진입 직전 |
| ProductSpec CRUD UI | Medium | Sprint 14 부속 (underlying VC/EX_CONSTRAINT 변경) |
| 장비/셋팅/합금형 5 entity CRUD UI | Medium | Sprint 14 EP-VC-FULL 부속 |
| Order 자동 INSERT 흐름 (ImportOrchestrator → mapping → diff → persist → commit chain) | Medium | Sprint 14 또는 carry-over — 현재 manual trigger |
| OrderCommittedNotificationListener Sprint 18 NotificationService 통합 | Low | Sprint 18 EP-NOTIFY |
| Diff jsonb fieldDiffs parsed object 변환 (현재 string) | Low | Sprint 14+ AG Grid 도입 시 |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.8](TASK-001_WBS_v1.8.md) — Sprint 12 EP-MASTER-UI 마감 Addendum
- [PLAN-SPRINT-13_EP-OC-FULL_v1.0](PLAN-SPRINT-13_EP-OC-FULL_v1.0.md) — Sprint 13 진입 plan (6 Story / 22 Task / DoD 10)
- [Sprint 13 commits chain](#) — `c39354b` (Day 1 Backend) → `2383a2d` (Day 2 Frontend) → `8d7c545` (Day 3 Notification)
- [V038 hotfix commit](#) — `08ab3eb` (Sprint 7 V033 audit trigger 누락 closure)
- [DiffController](../../backend/order/src/main/java/com/scheduling/order/api/DiffController.java)
- [OrderCommitController](../../backend/order/src/main/java/com/scheduling/order/api/OrderCommitController.java)
- [DiffPage.tsx](../../frontend/src/pages/DiffPage.tsx)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.2 | 2026-05-15 | (작성자) | 초안 ~ Phase 2 baseline (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | Sprint 7 carry-over EP-22·23 마감 |
| 1.4 | 2026-05-23 | Claude Code | Sprint 8 EP-V12-승인 + EP-V13-Grafana 마감 |
| 1.5 | 2026-05-27 | Claude Code | Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan (63 Epic / 341 SP) |
| 1.6 | 2026-05-27 | Claude Code | Sprint 10 EP-AUTH 100% 마감 (DoD 8/8) |
| 1.7 | 2026-05-27 | Claude Code | Sprint 11 EP-RBAC 100% 마감 (DoD 9/9 시각 검증) |
| 1.8 | 2026-05-27 | Claude Code | Sprint 12 EP-MASTER-UI 100% 마감 (DoD 8/12 + 4 carry-over) |
| 1.9 | 2026-05-27 | Claude Code | **Addendum — Sprint 13 EP-OC-FULL 100% 마감 (6 Story / 23 Task / 3 commits / ~5 SP). DiffController + OrderCommitController + DiffPage (severity 카드 + CRITICAL 빨강) + Commit UI (PLANNER reason Modal) + OrderCommittedNotificationListener (Sprint 18 진입점). Sprint 1~3 누적 자산 (DiffEngineService + SeverityClassifier + NotificationService) 활용. V038 hotfix (Sprint 7 V033 product_priority/kd_order audit trigger 누락 closure) 포함. DoD 9/10 ✅ + 1 carry-over (본 PC 실 엑셀 E2E). Sprint 14 EP-VC-FULL 진입 게이트 충족** |
