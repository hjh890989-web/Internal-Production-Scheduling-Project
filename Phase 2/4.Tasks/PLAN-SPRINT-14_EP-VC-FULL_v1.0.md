# Sprint 14 진입 계획 — EP-VC-FULL (성형 시뮬뷰 PDD-02 완성) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 14 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 14 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.9 §6 carry-over](TASK-001_WBS_v1.9.md) + REF-PDD-02 v1.1 (성형 프로세스 + BR-V07/V12/V13/V15/V16/V17) + [PLAN-SPRINT-13_EP-OC-FULL_v1.0](PLAN-SPRINT-13_EP-OC-FULL_v1.0.md)

---

## 1. 목적

**Sprint 13 EP-OC-FULL 직후 진입** — Order SSoT 확정된 상태에서 **성형 시뮬뷰 PDD-02 워크플로우 완성**:

1. **OrderCommittedListener** (vc 모듈) — Sprint 13 publisher 와 chain 완성. PLANNER 가 수주 확정 시 vc 가 자동으로 성형 스케줄 입력 단계 진입
2. **시뮬뷰 1500 row × 30 col 실 데이터 연결** — 현재 stub data → master + order commit 데이터로 채움
3. **dnd-kit 드래그** → 백엔드 변경 → **STOMP 실시간 broadcast** (현장 STK + Planner 화면 동기)
4. **Sprint 7~9 Capa/KD 통합** — 시뮬뷰 안에서 capa 초과 시 자동 capacity-queue 진입 + KD 보충 옵션 (현재 별도 페이지)

**현황 인벤토리:**
- ✅ Backend 자산 (Sprint 1~9 누적) — VcSchedule entity + Repository + 6 Controller + 17 sub-package (allocator/capacity/conflict/deadline/override/required/routing/rule/swap/validate/yield/...) + AllocatorChainListener + OrderChangedListener + VcConfirmedPublisher
- ✅ Frontend 자산 (Sprint 5 EP-15 + Sprint 7-9 누적) — VcSimulationPage + VcRotationGrid + VcGanttBoard + dnd-kit (Draggable/Droppable) + ViolationModal + OverrideJustificationForm + SwapProposalPanel + STOMP client (stompClient.ts)
- ⏳ Sprint 14 신설/통합 — OrderCommittedListener + 시뮬뷰 실 데이터 흐름 + AG Grid Enterprise (현재 Ant Table) + STOMP broadcast 통합 + Capa/KD UI 통합

**활성 후 효과:**
- PLANNER 가 수주 확정 → 시뮬뷰가 자동으로 성형 스케줄 입력 진입 (수동 trigger 제거)
- STK 가 시뮬뷰에서 swap 제안 → PLANNER 즉시 수용/거절 (현장 dual screen 동기)
- Capa 초과 시 capacity-queue 자동 진입 (Sprint 7~9 자산 활용)
- Sprint 15 EP-EX-FULL 진입 게이트 — 성형 schedule SSoT 확정 (압출 입력)

---

## 2. Sprint 14 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-VC-1 OrderCommittedListener (vc 모듈, Sprint 13 chain) | 0.7 | 0.4 |
| ST-VC-2 시뮬뷰 1500 row 실 데이터 연결 (시드 + Order commit 통합) | 1.5 | 0.8 |
| ST-VC-3 AG Grid Enterprise 도입 (현재 Ant Table → AG Grid) | 1.5 | 0.8 |
| ST-VC-4 STOMP 실시간 broadcast (PLANNER 변경 → STK 화면 1초 내 갱신) | 1.0 | 0.5 |
| ST-VC-5 Capa/KD 자동 통합 (시뮬뷰 안 capa 초과 시 자동 capacity-queue 진입) | 1.0 | 0.5 |
| ST-VC-6 VC-FULL IT 5 cases (E2E sample) | 0.5 | 0.3 |
| **합계** | **~6 SP** | **~3.3 PD** |

> **WBS v1.5 계획 6 SP 와 정합.**

---

## 3. 의존성 DAG

```
ST-VC-1 (OrderCommittedListener)
    ↓
ST-VC-2 (시뮬뷰 실 데이터) ──┐
                              │
ST-VC-3 (AG Grid Enterprise) ─┤
                              ↓
ST-VC-4 (STOMP broadcast) ─→ ST-VC-5 (Capa/KD 통합)
                                    ↓
                             ST-VC-6 (IT 5 cases) → DoD
```

**병렬 윈도우:**
- **ST-VC-2 ↔ ST-VC-3** — 백엔드/프론트 디커플링 (실 데이터 시드 vs AG Grid 통합)
- **ST-VC-4 ↔ ST-VC-5** — STOMP infra vs Capa/KD 로직 디커플링

---

## 4. Story · Task 매트릭스

### ST-VC-1 — OrderCommittedListener (Sprint 13 publisher 와 chain)

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-1-1 | OrderCommittedListener (vc.internal) — @ApplicationModuleListener AFTER_COMMIT async + log + (Sprint 14 baseline 은 LOG only, 실 schedule INSERT 는 ST-VC-2 시드 흐름) | 0.3 |
| TK-VC-1-2 | vc/package-info.java — allowedDependencies `order::events` 추가 (이미 있을 가능성 확인) | 0.1 |
| TK-VC-1-3 | OrderCommittedListenerIT — commit 후 listener 호출 검증 (Awaitility) | 0.3 |

### ST-VC-2 — 시뮬뷰 실 데이터 연결

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-2-1 | V039 hotfix (또는 별도 seed script) — 1주 horizon sample VcSchedule 100~500 row 시드 (master + order 시드 가정) | 0.4 |
| TK-VC-2-2 | VcScheduleQueryController `/slots` — 기존 그대로, 실 데이터 시드 후 자동 작동 | 0 (이미 완성) |
| TK-VC-2-3 | OrderCommittedListener → VcScheduleService.draft (auto-INSERT) 자동 흐름 — Sprint 14 baseline 은 manual seed, Phase 5+ 통합 | 0.5 |
| TK-VC-2-4 | 시뮬뷰 페이지 — 빈 grid 표시 시 "수주 확정 후 자동 입력" 안내 (현재 빈 grid 만) | 0.3 |
| TK-VC-2-5 | IT — VcScheduleQueryControllerIT (시드 후 from/to range query 검증) | 0.3 |

### ST-VC-3 — AG Grid Enterprise 도입

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-3-1 | frontend 의존성 검증 — `ag-grid-enterprise` + `ag-grid-react` (이미 vite.config manualChunks 에 있음, package.json 확인) | 0.2 |
| TK-VC-3-2 | VcRotationGrid 리팩터 — AG Grid 로 변경 (1500 row × 30 col 가상 스크롤 + 컬럼 pinning + 검색) | 0.7 |
| TK-VC-3-3 | dnd-kit ↔ AG Grid 통합 — AG Grid 의 native drag 또는 cellRenderer 안에 DraggableHose 임베드 | 0.4 |
| TK-VC-3-4 | unit test — AG Grid 의 column 정의 + cellRenderer | 0.2 |

### ST-VC-4 — STOMP 실시간 broadcast

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-4-1 | VcSchedule INSERT/UPDATE 후 STOMP `/topic/vc-schedule-updates` broadcast — Sprint 6 ExReplanPushListener 패턴 동일 | 0.5 |
| TK-VC-4-2 | Frontend — stompClient subscribe + VcSimulationPage 자동 reload (react-query invalidateQueries) | 0.3 |
| TK-VC-4-3 | IT — VcSchedule INSERT 후 STOMP message 발행 검증 (Awaitility) | 0.2 |

### ST-VC-5 — Capa/KD 자동 통합

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-5-1 | 시뮬뷰 안 capa 초과 시 자동 capacity-queue 진입 — 현재 별도 페이지 → ViolationModal 안에 "Capa 큐로 분리" 버튼 추가 | 0.4 |
| TK-VC-5-2 | KD 보충 통합 — supplement() 호출 버튼 (PLANNER 만) | 0.4 |
| TK-VC-5-3 | IT — VC simview 에서 capa 초과 시도 → capacity-queue 자동 분리 (CapacityOverflowQueueService.split 호출 검증) | 0.2 |

### ST-VC-6 — VC-FULL IT 5 cases

| Task | 내용 | SP |
|---|---|:--:|
| TK-VC-6-1 | OrderCommittedListenerIT 1 (Sprint 13 → 14 chain) | 0.2 |
| TK-VC-6-2 | VcScheduleQueryControllerIT 1 (range query) | 0.1 |
| TK-VC-6-3 | VcStompBroadcastIT 1 (Awaitility) | 0.1 |
| TK-VC-6-4 | CapacityOverflowIntegrationIT 1 (simview → split) | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ PLANNER 가 수주 확정 → vc OrderCommittedListener LOG 출력 (Sprint 14 baseline) 또는 실 schedule INSERT (Phase 5+ 통합)
2. ✅ 시뮬뷰에 1주 horizon 시드 데이터 (≥100 row) 표시
3. ✅ AG Grid 1500 row 가상 스크롤 + 컬럼 pinning + 검색 작동
4. ✅ PLANNER 가 dnd-kit 으로 hose 드래그 → 백엔드 변경 → STOMP 브로드캐스트 → STK 화면 1초 내 갱신
5. ✅ Capa 초과 시 자동 capacity-queue 진입 (Sprint 7~9 자산 재사용)
6. ✅ KD 보충 supplement() 호출 통합
7. ✅ 본 PC 실 시나리오 — PLANNER 가 수주 확정 → 시뮬뷰 진입 → 드래그 → STOMP 동기 확인

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend IT 5 신규 + 회귀 0
3. ✅ TypeScript compile + frontend unit tests GREEN
4. ✅ AG Grid 1500 row 렌더 성능 < 2s (NFR-PER-005)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| AG Grid Enterprise 라이선스 (운영 후 비용) | 라이선스 비용 | DEV 는 Community Edition 으로 시작 — Sprint 19 EP-BETA-LAUNCH 진입 시 Enterprise 라이선스 결정 |
| Order commit → vc schedule 자동 INSERT 흐름 복잡 | Sprint 14 일정 | Sprint 14 baseline 은 listener LOG only — manual seed 로 시뮬뷰 검증 (Phase 5+ 자동 흐름) |
| STOMP broadcast race condition (다중 사용자 동시 변경) | 시뮬뷰 동기 깨짐 | optimistic locking (VcSchedule version 컬럼 이미 있음) + Sprint 5/8 ExReplan 패턴 재사용 |
| 1500 row 렌더 성능 (NFR-PER-005) | UX 저하 | AG Grid 가상 스크롤 + 페이지네이션 50/100/500 옵션 + 검색 우선 (열기 시 50 row 만) |
| dnd-kit + AG Grid 통합 까다로움 | dnd 작동 안 함 | AG Grid native rowDrag 우선 — dnd-kit 은 cellRenderer 안에서 보조 (또는 carry-over Sprint 15) |
| EP-21 제약 (좌/우 호기 앵글 규격<7) 검증 누락 | 본 Sprint 검증 부족 | 기존 Sprint 5 ScheduleValidateController 와 SwapProposal 흐름이 이미 적용 — 본 Sprint 는 통합만 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Backend Listener + 시드:
1. TK-VC-1-1~3 (OrderCommittedListener + IT)
2. TK-VC-2-1 (시드 script)
3. TK-VC-2-5 (VcScheduleQueryControllerIT)

**Day 2** — Frontend AG Grid + STOMP:
4. TK-VC-3-1~4 (AG Grid 도입)
5. TK-VC-4-1~3 (STOMP broadcast)

**Day 3** — Capa/KD 통합 + DoD:
6. TK-VC-5-1~3 (Capa/KD UI 통합)
7. **DoD 본 PC 시각 검증** — 시드 → 시뮬뷰 → 드래그 → STOMP 동기 확인

**총 ~3.3 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Listener | `OrderCommittedListener.java` (vc.internal 신규) |
| Backend Migration | V039 (선택 — seed VcSchedule) |
| Backend Service | `VcStompPublisher.java` (notify 모듈 또는 vc 모듈) — Sprint 6 패턴 재사용 |
| Backend IT | `OrderCommittedListenerIT.java` + `VcScheduleQueryControllerIT.java` + `VcStompBroadcastIT.java` + `CapacityOverflowIntegrationIT.java` (4-5 신규) |
| Frontend | `VcRotationGrid.tsx` (AG Grid 리팩터) + `VcSimulationPage.tsx` (STOMP subscribe + Capa/KD 버튼) + `ViolationModal.tsx` (Capa 큐 분리 버튼) |
| Docs | rbac-matrix.md v1.2 부분 갱신 (§2.4 simview write endpoint 추가) |

---

## 9. Sprint 14 후 다음 단계

**Sprint 15 (EP-EX-FULL) 진입 조건:**
- ✅ DoD 11/11 충족
- ✅ 본 PC 실 시나리오 — PLANNER 수주 확정 → 시뮬뷰 진입 → 드래그 → STOMP 동기
- ✅ Vc schedule SSoT 확정 (Sprint 15 압출 입력)

**Sprint 15 첫 작업** — PLAN-SPRINT-15 작성 (압출 PDD-03 — 4-shift × 75% 매트릭스 + 다중 후보 ranking + BR-E01/E05).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-VC-FULL 6 Story / 22 Task / ~6 SP 분해 + 의존성 DAG + DoD 11 + 3-Day 작업 순서 + Sprint 1~9 누적 자산 (VcSchedule + 6 Controller + 17 sub-package + Frontend vc-scheduling 21 files) 활용 + OrderCommittedListener (Sprint 13 chain) + AG Grid + STOMP + Capa/KD 통합 |
