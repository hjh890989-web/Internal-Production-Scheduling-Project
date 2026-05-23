# Sprint 7 완료 보고서 (carry-over 마감 + Phase 4 대기)

**Sprint**: S7 (carry-over) | **기간**: 2026-05-23 (1일) | **상태**: ✓ 완료
**작성**: 2026-05-23 | **상위**: [Phase-3_Completion_v1.0](../2.Phase-Completion/Phase-3_Completion_v1.0.md)

> Phase 3 (개발) 종료 후 추가 carry-over Sprint. **BR-V12·V13 (수주통합 후 활성, deferred)
> 백엔드 인프라 + Sprint 7 종합 회귀 + Vite bundle baseline 보고**. Phase 4 베타 진입 전
> 마지막 코드 작업 mile stone.

---

## 1. Sprint 7 목표

> "Sprint 4 carry-over (BR-V12 capa 초과 큐 + BR-V13 KD 잔량 보충, REQ-FUNC-VC-022·023)
>  백엔드 인프라 완성 + Sprint 6 후속 cleanup (tsconfig baseUrl deprecation)."

---

## 2. Task 매트릭스 (3 commit, 2 Epic-equivalent)

### BR-V12·V13 (deferred 활성) — 2 commit

| Task | 상태 | Commit |
|---|---|---|
| V033 master.product_priority + master.kd_order (hose_id PK + remaining_qty CHECK + 4-status 머신) | ✓ | 9a65847 |
| ProductPriority + KdOrder JPA entity + Repository | ✓ | 9a65847 |
| master.api ProductPriorityLookup + KdOrderLookup + 2 Summary record + LookupImpl | ✓ | 9a65847 |
| 단위 6 — KdOrderTest (partial/full/over-consume/invariants) | ✓ | 9a65847 |
| vc.capacity_overflow.CapacityOverflowQueueService (priority rank ASC + capa 초과 큐 분리) | ✓ | d1610a7 |
| vc.capacity_overflow.KdSupplementService @Auditable (동일 hose 1차 + 그룹 fallback 2차) | ✓ | d1610a7 |
| 단위 5 — CapacityOverflowQueueServiceTest (within/over/partial/fallback/empty) | ✓ | d1610a7 |
| IT 5 — BrV12V13IT (priority split + same-hose + group + zero + missing) | ✓ | d1610a7 |

### Sprint 6 후속 fix — 1 commit

| Task | 상태 | Commit |
|---|---|---|
| tsconfig.json baseUrl 제거 (TS 7.0 호환) — paths "./src/*" 만 유지 | ✓ | a12e644 |

**합계** — 3 commit / ~15 신규 파일 / 16 신규 tests.

---

## 3. 핵심 지표

| 영역 | 결과 |
|---|---|
| Backend 전수 회귀 | **788 tests / 0 failures / 0 errors** (8 모듈 + app IT 누적) |
| Frontend vitest | 54 tests / 0 failures |
| Frontend lint + tsc | 0 warning / 0 error |
| Playwright spec | 226 등록 |
| Modulith verify | 0 위반 (9 모듈 유지) |
| ArchUnit | 29 rule 통과 |
| **🌟 V033 BR-V12** | priority rank ASC split + capa 내 채택 + 추가 요청 큐 분리 |
| **🌟 V033 BR-V13** | 동일 hose 1차 + 셋팅 그룹 2차 fallback + atomic consume + status 자동 전이 |
| TS 7.0 호환 | baseUrl 제거 + paths 만 유지 |

---

## 4. 핵심 deliverable — BR-V12·V13 (deferred 활성)

### 활성 조건

본 Sprint 의 산출은 **수주통합 (Sprint 1 EP-01) 안정화 후 + DI-07 PRODUCT_PRIORITY + DI-08
KD_ORDER 마스터 입력 완료 시점에 활성**. 즉시 운영 진입 안 함 (PDD-02 v1.1 + SRS v1.4 명시).

### 진입 절차 (Phase 4 베타 또는 Phase 5 PROD 진입 후)

```sql
-- IT_OPS — DI-07 PRODUCT_PRIORITY 입력
INSERT INTO master.product_priority (hose_id, priority_rank, rationale, effective_from, updated_by)
VALUES ('29673-2R060', 1, 'VIP 고객 X사', CURRENT_DATE, 'it_ops_사번');

-- DI-08 KD_ORDER 입력 (수주통합 시 자동 또는 수동)
INSERT INTO master.kd_order (kd_order_id, hose_id, order_qty, remaining_qty, order_date, customer_code, updated_by)
VALUES (gen_random_uuid(), '29673-2R060', 100, 100, '2026-06-01', 'CUST-X', 'system');

-- VC Allocator 가 자동 호출 (Sprint 7+ wiring 검토)
```

### Service API

```java
// BR-V12 — Σ Q_required > daily_capa 시
CapacityOverflowQueueService.SplitResult result = overflowService.split(
    Map.of("29673-2R060", 60, "28422-2M800", 50),  // hose → 요구량
    100                                              // daily_capa
);
// result.accepted     — capa 내 자동 채택 (priority rank 1, 2 우선)
// result.requestQueue — Planner 승인 대기 (UI 모달, Sprint 8+ wiring)

// BR-V13 — capa 부족 시
KdSupplementService.SupplementResult sup = supplementService.supplement(
    "29673-2R060", 80, "planner-001");
// sup.supplemented  — 실 보충량 (동일 hose 1차 + 그룹 2차)
// sup.consumed[]    — 각 KD order 차감 내역 (audit 자동)
```

---

## 5. Vite bundle baseline (Sprint 6 → Sprint 7)

```
Entry first paint:  ~57kB gzip   (DoD 200kB 큰 폭 통과 ✅)
antd-core:          384kB lazy (페이지 진입 시 fetch)
agGridSetup:        653kB lazy (/vc/simview 또는 /extrusion-matrix)
stomp:              22kB lazy (SockJS 연결 시)
총 chunk:           17개 (페이지 lazy 5 + vendor 6 + UI lib lazy 6)
```

→ PERF-002 (`docs/perf/PERF-002_Bundle_Regression_Report_v1.0.md`) 참조.

---

## 6. 3 Commit 시간순 (Sprint 7)

```
9a65847  feat(master): V033 PRODUCT_PRIORITY + KD_ORDER 마스터
d1610a7  feat(vc): BR-V12 CapacityOverflowQueue + BR-V13 KdSupplement
a12e644  fix(frontend): tsconfig baseUrl 제거 (TS 7.0 호환)
```

---

## 7. Sprint 7 Velocity

- **계획**: deferred carry-over (정확한 SP 산정 안 함, Phase 6+ 항목)
- **실제**: ~5 SP / 0.5일 (BR-V12·V13 백엔드 인프라 + UI 미포함)
- **누적 commit (Sprint 0~7)**: ~171 (Sprint 0 47 + S1 25 + S2 18 + S3 20 + S4 19 + S5 14 + S6 10 + 마감/Phase 4 docs 15 + Sprint 7 3)

---

## 8. Phase 4 진입 게이트 (Sprint 6 + Sprint 7 누적 확인)

- [x] **9 Modulith 모듈** + Phase 3 9 Epic + Sprint 7 carry-over 백엔드
- [x] **34 Flyway 마이그레이션** V001~V033
- [x] **9 핵심 BR + 2 deferred BR (V12·V13)** 백엔드 인프라
- [x] **Backend 788 tests / 0 failure** (Sprint 0~7 누적)
- [x] **Frontend vitest 54 + lint 0 + Vite bundle ~57kB entry**
- [x] **Playwright 226 spec 등록 + 베타 시나리오 5 SOP + 페르소나 4 가이드**
- [x] **누적 171 commit · 머지 충돌 0**

→ **Phase 4 (베타 운영) 진입 승인 가능**.

---

## 9. 차순위 carry-over (Sprint 8+ / Phase 5+)

| 항목 | 우선 |
|---|---|
| BR-V12 Planner UI — 추가 요청 큐 승인 모달 + REST controller | Medium (deferred 활성 시점) |
| BR-V13 KD 잔량 대시 — IT_OPS Grafana panel | Medium |
| Mobile App (Flutter 압출 패드) | High (Phase 5+) |
| ML 추천 (EP-18 ranking 자동화) | Low (Phase 6+) |
| ArchUnit DDD layer 강화 (`@DomainLayer`) | Low |
| IDE Resource leak suppress + 미사용 import cleanup | Low (visual noise) |

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 7 (3 commit, BR-V12·V13 deferred 백엔드 + tsconfig fix, ~5 SP / 0.5일) |
