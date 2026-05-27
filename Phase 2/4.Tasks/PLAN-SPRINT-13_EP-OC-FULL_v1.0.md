# Sprint 13 진입 계획 — EP-OC-FULL (수주통합 PDD-01 완성) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 13 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 13 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.8 §5 carry-over](TASK-001_WBS_v1.8.md) + REF-PDD-01 (수주통합 프로세스) + [rbac-matrix.md v1.1 §2.2](../../docs/security/rbac-matrix.md)

---

## 1. 목적

**Sprint 12 EP-MASTER-UI 직후 진입** — 마스터 데이터 운영 가능해진 상태에서 **수주통합 PDD-01 워크플로우 완성**:

1. Excel 업로드 → 파싱 → diff 분류 → **Planner 가 critical row 확인 + 확정** 흐름 통합
2. **Diff API + UI 페이지** 신설 — Sprint 1~3 누적 자산 (DiffEngineService + SeverityClassifier + DiffPersistenceService) 을 사용자가 시각적으로 활용
3. **Order Commit API** + UI 신설 — Diff 검토 후 PLANNER 가 commit (BR-O02 critical 우선 처리)
4. 알림 stub (Sprint 18 카카오 통합 전 baseline) — critical count in-app 알림

**현황 인벤토리:**
- ✅ **이미 완성** — ExcelParserService (SXSSF) · SchemaMappingService (룰 기반) · DuplicateDetectionService · PrecedenceResolver · DiffEngineService · SeverityClassifier (BR-O02 ±20% + 납기/hose/new/deleted always-critical) · DiffPersistenceService · OrderCommitService
- ✅ **이미 완성** — OrderImportController (4 endpoint) · MappingRuleController · ExportController · OrderImportPage.tsx (업로드 + 매핑 검토 모달)
- ⏳ **Sprint 13 신설** — DiffController · OrderCommitController · DiffPage.tsx (severity 별 row + critical 강조) · Commit UI · 알림 stub

**활성 후 효과:**
- PLANNER 가 매일 영업/관리 부서 엑셀 받아 1-2분 내 critical row 식별 + 확정
- BR-O02 ±20% / 납기 변경 / hose ID 변경 / new / deleted 자동 분류 → 사용자 처리
- Sprint 14 EP-VC-FULL 진입 게이트 — 수주 데이터 SSoT 확정 (성형 스케줄 입력)

---

## 2. Sprint 13 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-OC-1 DiffController + DiffSummary endpoint | 0.5 | 0.3 |
| ST-OC-2 OrderCommitController + commit/reject endpoint | 0.7 | 0.4 |
| ST-OC-3 DiffPage.tsx — severity 별 Table + critical 강조 | 1.5 | 0.8 |
| ST-OC-4 Commit UI (DiffPage 내 확정 버튼) + 통합 흐름 | 1.0 | 0.5 |
| ST-OC-5 Notification stub Service + critical count 알림 | 0.5 | 0.3 |
| ST-OC-6 OC-FULL IT 6 cases (E2E sample 포함) | 0.8 | 0.4 |
| **합계** | **~5 SP** | **~2.7 PD** |

> **WBS v1.5 계획 5 SP 와 정합.**

---

## 3. 의존성 DAG

```
ST-OC-1 (DiffController)
    ↓
ST-OC-2 (OrderCommitController) ──┐
    ↓                              │
ST-OC-3 (DiffPage frontend)        │
    ↓                              ↓
ST-OC-4 (Commit UI 통합) ─────→ ST-OC-5 (Notification stub)
                                   ↓
                          ST-OC-6 (IT 6 cases) → DoD 검증
```

**병렬 윈도우:**
- **ST-OC-1 ↔ ST-OC-2** — Backend Controller 2 종 동시 작업
- **ST-OC-3 (frontend) ↔ ST-OC-5 (notification stub)** — UI/notification 디커플링

---

## 4. Story · Task 매트릭스

### ST-OC-1 — DiffController (Sprint 1~3 자산 endpoint 노출)

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-1-1 | DiffController (order.api 패키지) — GET /api/v1/orders/{trackingId}/diff → DiffSummaryResponse (severity 별 count + RowDiff list) | 0.3 |
| TK-OC-1-2 | DiffSummaryResponse record — totalRows / criticalCount / importantCount / standardCount / rows (RowDiff DTO) + RBAC (PLANNER + IT_OPS + READ_ONLY read) | 0.1 |
| TK-OC-1-3 | DiffPersistenceService 에 read API 추가 (findByTrackingId returning RowDiff list) — 기존 persist 만 있다면 | 0.1 |

### ST-OC-2 — OrderCommitController (확정/거절 endpoint)

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-2-1 | OrderCommitController — POST /api/v1/orders/{trackingId}/commit (PLANNER only, BR-X02 audit) + POST /reject | 0.3 |
| TK-OC-2-2 | OrderCommitService 확장 — commit() 가 OrderCommittedEvent 발행 (Sprint 14 EP-VC-FULL 진입점) + reject() | 0.3 |
| TK-OC-2-3 | CommitPayload record (reason 필수 BR-X02) + reject reason 필수 | 0.1 |

### ST-OC-3 — DiffPage.tsx (severity 별 Table)

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-3-1 | DiffPage.tsx — useDiffSummary hook + severity tag color (CRITICAL=red / IMPORTANT=orange / STANDARD=blue) + 통계 카드 (총/critical/important/standard) | 0.5 |
| TK-OC-3-2 | RowDiff Table — hose_id / 변경 필드 / before / after / severity / reason. CRITICAL row 상단 정렬 + 빨간 배경 highlight | 0.5 |
| TK-OC-3-3 | api/orderDiffApi.ts — fetch wrapper (list + 단일 row 검색) | 0.2 |
| TK-OC-3-4 | OrderImportPage.tsx — MappingReviewModal `onProceed` → navigate `/orders/diff/{trackingId}` (Sprint 1 stub 메시지 제거) | 0.2 |
| TK-OC-3-5 | router /orders/diff/{trackingId} 신설 + ProtectedRoute (4 role read) | 0.1 |

### ST-OC-4 — Commit UI 통합

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-4-1 | DiffPage 에 [확정] / [거절] 버튼 (PLANNER role 만 visible) + reason Modal | 0.4 |
| TK-OC-4-2 | api/orderDiffApi.ts 에 commit / reject method 추가 | 0.1 |
| TK-OC-4-3 | 성공 토스트 + audit_log.actor 자동 (BR-X02 ApprovalService 동일 패턴) + navigate /home | 0.2 |
| TK-OC-4-4 | useAuthStore.hasRole('PLANNER') 가드 — STK_USER/READ_ONLY 는 read only (확정 버튼 비활성) | 0.1 |
| TK-OC-4-5 | 단위 test — DiffPage commit 흐름 + role 가드 | 0.2 |

### ST-OC-5 — Notification stub

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-5-1 | NotificationStubService (notify 모듈) — critical count > 0 시 in-app 알림 발행 (PLANNER 대상). 카카오는 Sprint 18 | 0.3 |
| TK-OC-5-2 | DiffPersistenceService 의 OrderDiffPersistedEvent listener 추가 — diff 완료 시 stub 호출 | 0.1 |
| TK-OC-5-3 | 단위 test — critical count 0 → 알림 0, critical count 5 → 알림 1 | 0.1 |

### ST-OC-6 — OC-FULL IT 6 cases (E2E 통합)

| Task | 내용 | SP |
|---|---|:--:|
| TK-OC-6-1 | DiffControllerIT 2 — GET happy (severity 분류 정확) + 미존재 trackingId 404 | 0.3 |
| TK-OC-6-2 | OrderCommitControllerIT 3 — commit 200 + audit_log.actor 검증 / reject 200 + reason / STK_USER 403 | 0.3 |
| TK-OC-6-3 | NotificationStubIT 1 — critical count 5 → 알림 1건 발행 검증 (Awaitility) | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ Excel 업로드 → 매핑 → diff 페이지 자동 진입 (OrderImportPage.onProceed)
2. ✅ DiffPage severity 카드 4종 (총/CRITICAL/IMPORTANT/STANDARD) 표시
3. ✅ CRITICAL row 빨간 배경 + 상단 정렬
4. ✅ PLANNER 가 [확정] 버튼 → reason 입력 → 200 + audit_log.actor=사번
5. ✅ STK_USER/READ_ONLY 진입 시 read only (확정 버튼 비활성 또는 404)
6. ✅ critical count > 0 시 in-app 알림 1건 발행 (카카오는 Sprint 18 stub)
7. ✅ 본 PC 실 엑셀 1개 업로드 → 파싱 → diff → 확정 E2E 흐름 정상

**비기능 DoD:**
1. ✅ ArchUnit (PreAuthorize + Modulith) GREEN
2. ✅ Backend 신규 IT 6 cases GREEN + 회귀 0건
3. ✅ TypeScript compile + frontend unit tests GREEN

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| DiffEngineService 가 Sprint 1~3 작성 — 실 데이터 검증 부족 | 본 Sprint 통합 시 발견 | TK-OC-6 IT 가 실 Excel sample 사용 + 시드 row 비교 |
| Order 테이블 audit trigger 누락 가능성 (V038 hotfix 교훈) | BR-X02 위반 | TK-OC-2-2 commit 후 audit_log row 확인 IT 필수 |
| DiffSummaryResponse payload 큼 (1500 row) | 응답 지연 | 페이지네이션 — 초기 critical 만 50개 + 더보기 (Sprint 14 AG Grid 도입 전 임시) |
| OrderCommittedEvent 발행 시 vc 모듈 listener 없음 | Sprint 14 까지 noop | event 만 발행 (listener 는 Sprint 14 EP-VC-FULL) — 본 Sprint 영향 0 |
| Notification stub 이 in-app STOMP 통합 안 됨 | 알림 보이지 않음 | NotificationStubService 가 simple LOG 출력 + DB persist (Sprint 18 에서 STOMP 통합) |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Backend Diff + Commit:
1. TK-OC-1-1~3 (DiffController)
2. TK-OC-2-1~3 (OrderCommitController)

**Day 2** — Frontend Diff + Commit UI:
3. TK-OC-3-1~5 (DiffPage + router)
4. TK-OC-4-1~5 (Commit UI 통합)

**Day 3** — Notification + IT + DoD:
5. TK-OC-5-1~3 (Notification stub)
6. TK-OC-6-1~3 (IT 6 cases)
7. **DoD 본 PC 시각 검증** — 실 엑셀 업로드 → diff 페이지 → PLANNER 확정 → audit_log 확인

**총 ~2.7 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Controller | `DiffController.java`, `OrderCommitController.java` (2 신규) |
| Backend Service | `OrderCommitService.java` 확장 (event 발행), `NotificationStubService.java` (notify 모듈 1 신규) |
| Backend Event | `OrderCommittedEvent.java` (events 패키지 1 신규) |
| Backend IT | `DiffControllerIT.java` (2 cases), `OrderCommitControllerIT.java` (3), `NotificationStubIT.java` (1) — 신규 6 IT |
| Frontend Page | `DiffPage.tsx`, `OrderImportPage.tsx` (navigate 추가) |
| Frontend api | `orderDiffApi.ts` (1 신규) |
| Router | `router/index.tsx` (/orders/diff/{trackingId} 라우트) |
| Docs | rbac-matrix.md v1.2 부분 갱신 (§2.2 diff/commit 2 endpoint 추가) |

---

## 9. Sprint 13 후 다음 단계

**Sprint 14 (EP-VC-FULL) 진입 조건:**
- ✅ DoD 10/10 충족
- ✅ 본 PC 실 엑셀 E2E 흐름 검증
- ✅ Order 데이터 SSoT 확정 (Sprint 14 의 입력)

**Sprint 14 첫 작업** — PLAN-SPRINT-14 작성 (성형 시뮬뷰 PDD-02 완성 — AG Grid 1500×30 + dnd-kit + STOMP + Capa/KD 통합).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-OC-FULL 6 Story / 22 Task / ~5 SP 분해 + 의존성 DAG + DoD 10 + 3-Day 작업 순서 + Sprint 1~3 누적 자산 (parser/mapping/diff/domain/commit) 활용 + Diff/Commit endpoint + DiffPage 신설 |
