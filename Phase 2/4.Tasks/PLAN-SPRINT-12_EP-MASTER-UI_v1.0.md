# Sprint 12 진입 계획 — EP-MASTER-UI (마스터 데이터 입력 UI) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 12 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 12 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.6 §5 carry-over](TASK-001_WBS_v1.6.md) (사용자 PIN 변경 UI) + [WBS v1.7 §5 carry-over](TASK-001_WBS_v1.7.md) + [rbac-matrix.md v1.1 §3](../../docs/security/rbac-matrix.md) (Sprint 12 endpoint 계획)

---

## 1. 목적

**Sprint 11 EP-RBAC 직후 진입** — IT_OPS 가 마스터 데이터 직접 관리 가능. 베타 운영 직전 필수:

1. **사용자 관리** (Sprint 10 carry-over) — IT_OPS 가 신규 사용자 추가 + PIN reset + 잠금 해제 (SQL 직접 우회)
2. **PRODUCT_PRIORITY** — Sprint 7 V033 시드 데이터의 운영 변경 (BR-V12 capacity overflow 가 매번 사용)
3. **KD_ORDER** — Sprint 7 V033 시드 데이터의 운영 변경 (BR-V13 KD 보충이 매번 사용)
4. **47 품번 조회** — PLANNER/STK_USER 가 품번 spec 확인
5. **마스터 Hub 페이지** — IT_OPS 메뉴 활성 (disabled 해제 + 4 카드 진입점)

**활성 후 효과**:
- 베타 운영 중 사용자 PIN 분실 시 IT_OPS 가 즉시 reset (현재 PSQL SQL 수동 작업)
- Planner 가 새 우선순위 정책 적용 시 IT_OPS 가 UI 로 즉시 변경 (현재 SQL 수동)
- Sprint 13 EP-OC-FULL 진입 게이트 — 품번/우선순위/KD 마스터 모두 UI 운영 가능

**범위 제외 (carry-over)**:
- 장비 (LP/IC) CRUD → Sprint 14 EP-VC-FULL 부속
- 셋팅 그룹 / 합금형 / 회전수 → Sprint 14
- 휴일 캘린더 → 이미 HolidayController 있음, UI 만 신설 (Sprint 13+ carry-over)
- ProductSpec **CRUD** → Sprint 13 (Sprint 12 는 read 만)

---

## 2. Sprint 12 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-MASTER-1 마스터 Hub 페이지 + 메뉴 활성 | 0.5 | 0.3 |
| ST-MASTER-2 AppUser CRUD UI (Sprint 10 carry-over) | 1.5 | 0.8 |
| ST-MASTER-3 ProductPriority CRUD UI | 1.0 | 0.5 |
| ST-MASTER-4 KdOrder CRUD UI | 1.0 | 0.5 |
| ST-MASTER-5 ProductSpec read 페이지 (47 품번) | 0.5 | 0.3 |
| **합계** | **~4.5 SP** | **~2.4 PD** |

> **WBS v1.5 계획 5 SP 대비 -0.5 SP** (장비/셋팅 등 5개 entity 는 Sprint 14 carry-over — 베타 직전 OC/VC 우선순위 정합).

---

## 3. 의존성 DAG

```
ST-MASTER-1 (Hub + 메뉴 활성)
    ↓
    ├─→ ST-MASTER-2 (AppUser CRUD)     ─┐
    ├─→ ST-MASTER-3 (ProductPriority)  ─┤
    ├─→ ST-MASTER-4 (KdOrder)          ─┤
    └─→ ST-MASTER-5 (ProductSpec read) ─┘
                                          ↓
                                       DoD 검증 (본 PC IT_OPS 시각)
```

**병렬 윈도우**: ST-MASTER-2~5 모두 독립 (entity 별 CRUD). Backend Controller + Frontend Page 패턴 동일 → 한 묶음 작업 효율 ↑.

---

## 4. Story · Task 매트릭스

### ST-MASTER-1 — 마스터 Hub 페이지 + 메뉴 활성

| Task | 내용 | SP |
|---|---|:--:|
| TK-MASTER-1-1 | MasterHubPage.tsx — Ant Card grid (사용자/우선순위/KD/품번 4 카드 + Sprint 14 자리 placeholder). 각 카드 → navigate(/master/{section}) | 0.3 |
| TK-MASTER-1-2 | MainLayout 메뉴 `/master` disabled 제거 + allowedRoles=[IT_OPS] (이미 정합, disabled 만 해제) | 0.1 |
| TK-MASTER-1-3 | router /master/* 라우트 신설 (RoleGuard IT_OPS) + 4 페이지 lazy import | 0.1 |

### ST-MASTER-2 — AppUser CRUD UI (Sprint 10 TK-AUTH-7 carry-over 흡수)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MASTER-2-1 | UserAdminController (security.auth 패키지) — GET /api/v1/master/user (list), POST (create), POST /{empId}/reset-pin, POST /{empId}/unlock, DELETE /{empId}. RBAC: IT_OPS only | 0.4 |
| TK-MASTER-2-2 | UserAdminService — createUser (PIN BCrypt 인코딩) + resetPin + unlock + delete (@Auditable BR-X02) | 0.3 |
| TK-MASTER-2-3 | Frontend UserAdminPage.tsx — Ant Table (사번/role/잠금상태/실패횟수/생성일) + Modal (신규 사용자 + PIN reset + 잠금 해제 확인) | 0.5 |
| TK-MASTER-2-4 | api/userAdminApi.ts — fetch wrapper 5 method + types | 0.1 |
| TK-MASTER-2-5 | IT 5 cases — list/create/resetPin/unlock 정상 + STK_USER 403 + audit_log actor 검증 | 0.2 |

### ST-MASTER-3 — ProductPriority CRUD UI

| Task | 내용 | SP |
|---|---|:--:|
| TK-MASTER-3-1 | ProductPriorityController (master.priority 패키지) — GET (PLANNER+IT_OPS+READ_ONLY), POST/PUT/DELETE (IT_OPS) | 0.3 |
| TK-MASTER-3-2 | ProductPriorityAdminService — create/update/delete (effectiveAt 정합, valid_from/to 범위 검증) + @Auditable | 0.2 |
| TK-MASTER-3-3 | Frontend ProductPriorityPage.tsx — Ant Table (hose_id/rank/segment/valid_from/to) + Modal (신규/수정) + drag-and-drop rank 재정렬 (옵션) | 0.4 |
| TK-MASTER-3-4 | api/productPriorityApi.ts + IT 3 cases | 0.1 |

### ST-MASTER-4 — KdOrder CRUD UI

| Task | 내용 | SP |
|---|---|:--:|
| TK-MASTER-4-1 | KdOrderController (master.kd) — GET/POST/PUT/DELETE (IT_OPS write, 4 role read) | 0.3 |
| TK-MASTER-4-2 | KdOrderAdminService — create/update/delete (remaining_qty 비음수 검증) + @Auditable | 0.2 |
| TK-MASTER-4-3 | Frontend KdOrderPage.tsx — Ant Table (hose_id/order_qty/remaining_qty/order_date) + Modal | 0.4 |
| TK-MASTER-4-4 | api/kdOrderApi.ts + IT 3 cases | 0.1 |

### ST-MASTER-5 — ProductSpec read 페이지 (47 품번)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MASTER-5-1 | ProductSpecController (master.spec) — GET list + GET /{hoseId} (4 role read) | 0.2 |
| TK-MASTER-5-2 | Frontend ProductSpecPage.tsx — Ant Table 조회 only (hose_id/규격/계열/...) + 검색 + 필터 | 0.2 |
| TK-MASTER-5-3 | api/productSpecApi.ts + IT 2 cases | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD**:
1. ✅ IT_OPS (`00000007/0007`) 로그인 → 마스터 메뉴 클릭 → MasterHubPage 4 카드 표시
2. ✅ 사용자 카드 클릭 → UserAdminPage → 8명 시드 사용자 list + 신규 사용자 추가 + PIN reset + 잠금 해제 작동
3. ✅ 우선순위 카드 → ProductPriorityPage → 시드 데이터 list + 수정 → BR-V12 capacity overflow 즉시 반영 검증
4. ✅ KD 카드 → KdOrderPage → 시드 데이터 list + 수정 → BR-V13 KD 보충 즉시 반영 검증
5. ✅ 품번 카드 → ProductSpecPage → 47 품번 list + 검색 작동
6. ✅ PLANNER (`00000001`) → 마스터 메뉴 안 보임 (rbac-matrix 정합)
7. ✅ STK_USER (`00000004`) → /master 직접 진입 → /forbidden redirect
8. ✅ audit_log.actor = 사번 (IT_OPS 변경 시 모두 actor=00000007 기록)

**비기능 DoD**:
1. ✅ ArchUnit (PreAuthorize + Modulith) GREEN
2. ✅ Backend IT 신규 13 cases GREEN + 회귀 0건
3. ✅ TypeScript compile + frontend unit tests GREEN
4. ✅ Smoke 마스터 4 페이지 — 본 PC 로그인 후 정상 로드 + 콘솔 에러 0건

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| PIN reset 시 평문 PIN 노출 (응답 body) | NFR-SEC-005 위반 | 응답에 PIN 표시 안 함 — IT_OPS 가 별도 채널로 사용자에게 전달 (모달에 1회만 표시 + clipboard copy 옵션) |
| 우선순위 변경 후 cache 미동기 → BR-V12 stale | BR-V12 정합 위반 | ProductPriorityLookup cache 강제 무효화 (Sprint 8 SlotCompatibilityMatrix LISTEN/NOTIFY 패턴 재사용) — Sprint 13 carry-over 가능 |
| 신규 사용자 emp_id 중복 (8자리) | 시드 충돌 | UserAdminService.createUser 가 PK violation 잡아 400 + 한국어 메시지 |
| ProductSpec list 47 품번 페이지 너무 큼 | UX | Ant Table virtualScroll + 검색 + 필터 (Sprint 14 EP-VC-FULL AG Grid 도입 전 임시) |
| ProductPriority drag-and-drop rank 재정렬 복잡 | Sprint 12 일정 | 본 Sprint 는 단순 input rank — drag-drop 은 Sprint 13+ 옵션 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Hub + AppUser CRUD:
1. TK-MASTER-1-1~3 (Hub + 메뉴 활성 + router)
2. TK-MASTER-2-1~5 (UserAdmin backend + frontend + IT)

**Day 2** — Priority + KD:
3. TK-MASTER-3-1~4 (ProductPriority)
4. TK-MASTER-4-1~4 (KdOrder)

**Day 3** — ProductSpec + DoD 검증:
5. TK-MASTER-5-1~3 (ProductSpec read)
6. **DoD 본 PC 시각 검증** — IT_OPS 4 카드 + 우선순위 수정 → BR-V12 즉시 반영 확인

**총 ~2.4 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Controller | `UserAdminController.java`, `ProductPriorityController.java`, `KdOrderController.java`, `ProductSpecController.java` (4 신규) |
| Backend Service | `UserAdminService.java`, `ProductPriorityAdminService.java`, `KdOrderAdminService.java` (3 신규) |
| Backend IT | `UserAdminIT.java` (5 cases), `ProductPriorityAdminIT.java` (3 cases), `KdOrderAdminIT.java` (3 cases), `ProductSpecIT.java` (2 cases) — 신규 13 IT |
| Frontend Page | `MasterHubPage.tsx`, `UserAdminPage.tsx`, `ProductPriorityPage.tsx`, `KdOrderPage.tsx`, `ProductSpecPage.tsx` (5 신규) |
| Frontend api | `userAdminApi.ts`, `productPriorityApi.ts`, `kdOrderApi.ts`, `productSpecApi.ts` (4 신규) |
| Router/Layout | `router/index.tsx` (/master/* 4 라우트 + RoleGuard), `MainLayout.tsx` (disabled 제거), `i18n` (메뉴 라벨) |
| Docs | rbac-matrix.md v1.2 부분 갱신 (§3 → §2 신규 endpoint 8건 이동) |

---

## 9. Sprint 12 후 다음 단계

**Sprint 13 (EP-OC-FULL) 진입 조건**:
- ✅ DoD 12/12 충족
- ✅ IT_OPS 가 본 PC 에서 마스터 4종 모두 변경 가능
- ✅ 우선순위 변경 → BR-V12 즉시 반영 검증

**Sprint 13 첫 작업** — PLAN-SPRINT-13 작성 (수주통합 PDD-01 완성 — Excel 업로드 → 파싱 → severity 분류 → 수정요청 워크플로우).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-MASTER-UI 5 Story / 19 Task / ~4.5 SP 분해 + 의존성 DAG + DoD 12 + 3-Day 작업 순서 + Sprint 10 TK-AUTH-7 (PIN 관리) carry-over 흡수 + 장비/셋팅 5 entity Sprint 14 carry-over |
