# ADR-022 — Sprint 7 carry-over 결정사항 종합 (v1.0)

**Status**: Accepted | **Date**: 2026-05-23 | **Authors**: Claude Code
**Sprint**: S7 (carry-over — BR-V12·V13 풀 스택) | **상위 ADR**: ADR-008~021 (Phase 2 SAD-001 + Sprint 6)

> Sprint 7 carry-over (BR-V12·V13 deferred 활성) 도중 결정된 **5개 architecture decision** 영구
> 기록. ADR-008~021 위에서 capa 초과/부족 도메인 모델 + REST + UI 본격 활성 시 결정.
> ADR-021 동일 패턴 — **in-place 금지 원칙** 으로 SAD-001 본문 수정 대신 별도 파일.

---

## ADR-022-A — V033 master.api facade pattern (ProductPriorityLookup + KdOrderLookup)

### Context

BR-V12 capa 초과 split 알고리즘은 PRODUCT_PRIORITY 마스터 조회 필요. BR-V13 KD 보충은
KD_ORDER 마스터 조회 + remaining_qty 차감 필요. **vc 모듈이 master 모듈 내부 entity 를
직접 import 하면 Modulith 모듈 경계 위반** (ApplicationModule allowedDependencies).

### Decision

- **facade interface** `master.api.ProductPriorityLookup` + `master.api.KdOrderLookup` 정의 (DTO record 반환)
- **구현** `master.priority.ProductPriorityLookupImpl` + `master.kd.KdOrderLookupImpl` (@Component @Profile("with-infra"))
- **DTO record** `master.api.ProductPrioritySummary` + `master.api.KdOrderSummary` (불변)
- vc 모듈은 `master::api` 만 의존 — Modulith 경계 유지

### Status

✅ Accepted. ModuleBoundaryTest 9 모듈 + Modulith verify 0 위반 통과 (Sprint 7 carry-over 후 회귀).

### Consequences

- **+** master 내부 entity (JPA) 노출 차단 — 캡슐화 유지
- **+** 향후 master 리팩토링 시 vc 영향 0 (facade 시그니처만 유지)
- **−** facade + Impl + DTO 3 파일 보일러플레이트 (Sprint 0 이래 ExConstraintLookup·SettingGroupLookup·HoseRuleLookup 등 일관 패턴)

---

## ADR-022-B — capacity_overflow 패키지 위치 (vc 모듈 내부 신규 패키지, 새 Modulith 모듈 X)

### Context

BR-V12·V13 의 CapacityOverflowQueueService + KdSupplementService 는 Allocator 호출 chain 일부 —
vc 모듈 내부 도메인. 새 Modulith 모듈 (e.g. `com.scheduling.capacity_overflow`) 분리 검토 했음.

### Decision

- **package** `vc.capacity_overflow` 신규 (vc 모듈 내부)
- **이유**:
  - capa 분기 로직은 vc Allocator 의 자연스러운 확장 (별 모듈 가치 낮음)
  - Modulith 모듈 추가 → ModuleBoundaryTest expected 변경 (9 → 10) 필요, Phase 2 SAD §3 컨테이너 매트릭스 갱신 필요 — overkill
  - ADR-021 의 kpi 모듈 (Sprint 6 EP-47) 처럼 cross-module event subscriber 가 없는 한 모듈 분리 미정당

### Status

✅ Accepted. 9 Modulith 모듈 유지 (common·master·order·vc·ex·audit·notify·security·kpi).

### Consequences

- **+** Modulith 모듈 수 9 안정 유지 — Phase 2 SAD §3 변경 0
- **+** vc 내부 단일 책임 — capa 분기 로직 + Allocator + 5 룰 한 모듈 응집
- **−** vc 모듈 size 증가 (단, 다른 모듈 (ex 13 컴포넌트) 보다 작음)

---

## ADR-022-C — BR-V13 KdSupplementService `@Auditable` AOP 사용 결정

### Context

BR-V13 supplement 는 KD_ORDER `remaining_qty` 차감 — DB mutation. **BR-X02 mutation audit
강제** 적용 필요. 옵션:
- (i) `@Auditable` AOP (Sprint 4 EP-11 표준 패턴, ScheduleAuditedEvent 발행)
- (ii) DB trigger (V025 audit trigger 같은 SQL 레벨)
- (iii) inline `auditRepository.save(...)` 명시 호출

### Decision

- **(i) `@Auditable` AOP** 채택 — `KdSupplementService.supplement` method 에 annotation
- ScheduleAuditedEvent 발행 → audit.schedule_audit_log INSERT (V025 trigger 와 동일 row 구조)
- principal (`@AuthenticationPrincipal` 또는 inline `Principal` param) 자동 attribution

### Status

✅ Accepted. CapacityOverflowControllerIT — POST /supplement PLANNER 200 + audit principal 검증 PASSED.

### Consequences

- **+** Sprint 4 표준 패턴 일관성 — Allocator·Override·Confirm·Swap 등과 동일 AOP
- **+** DB trigger 변경 0 — V033 신규 테이블 (kd_order) 도 V025 trigger 적용 범위 자동 포함 (table_name='kd_order' filter)
- **−** trigger 만이었으면 controller bypass 시 audit 가능했으나, application layer audit 만 → DB 직접 UPDATE 시 audit 누락 가능 (단, V025 trigger 가 kd_order 적용 — 이중 보장)

---

## ADR-022-D — REST endpoint `@PreAuthorize("hasRole('PLANNER')")` 단독 (BR-X05 dual-review 정합)

### Context

CapacityOverflowController POST /split 과 /supplement RBAC 결정. 옵션:
- (i) PLANNER 단독 (작성자 + 승인자 = 동일인)
- (ii) PLANNER + STK_USER (제안 + 승인 분리)
- (iii) PLANNER + IT_OPS (시드 + 운영 분리)

### Decision

- **(i) PLANNER 단독** 채택 — `@PreAuthorize("hasRole('PLANNER')")` 양 endpoint
- BR-X05 dual-review 는 **다른 작성자/승인자** 의 분리이지, **같은 사람이 split + supplement 액션 모두 수행** 차단이 아님
- STK_USER 의 swap 제안 → Planner 수용 패턴과 다름 (V12·V13 은 swap 이 아닌 capa 분기 + 마스터 차감)

### Status

✅ Accepted. CapacityOverflowControllerIT — STK_USER 403 + READ_ONLY 403 + 미인증 401 모두 PASSED. PLANNER 단독 정합.

### Consequences

- **+** RBAC 명확성 — Planner 가 capa 결정 단일 책임
- **+** BR-X05 dual-review 는 confirm/override (다른 endpoint) 에 한정 — V12·V13 은 운영 결정
- **−** Planner 부담 증가 — 향후 BR-V12 추가 요청 큐 **승인** 워크플로우 (Sprint 8+) 에서 STK_USER 제안 + Planner 승인 분리 가능

---

## ADR-022-E — Frontend SPA route `/vc/capacity-queue` + Tabs (AntD)

### Context

BR-V12 + BR-V13 은 별 UI workflow 이지만 Planner 가 capa 부족 시점에 같이 사용 (initial overflow detection → priority split 결정 → KD remaining 보충). 옵션:
- (i) 단일 페이지 + Tabs (V12 Split + V13 KD 보충)
- (ii) 2 라우트 분리 (`/vc/capacity-overflow` + `/vc/kd-supplement`)
- (iii) `/vc/simview` 기존 페이지 통합 (panel 확장)

### Decision

- **(i) 단일 페이지 `/vc/capacity-queue` + AntD Tabs** 채택
- Tab1 BR-V12 split 미리보기 + Tab2 BR-V13 KD 보충
- 진입점 — MainLayout 메뉴 `menu.capacityQueue` (i18n ko/en)
- AntD `Tabs` 컴포넌트 — 신규 청크 `CapacityQueuePage` 2.72kB gzip (lazy, NFR-PER-005 영향 +0.10kB)

### Status

✅ Accepted. Frontend lint 0 + vitest 58 (capacityOverflow.types 4 신규) + Vite build 14.79s 통과.

### Consequences

- **+** Planner workflow 일관성 — capa 분기 + 부족 처리 같은 화면
- **+** chunk 분리 양호 — lazy 진입 (다른 페이지 진입 시 fetch 0)
- **+** 라우트 단순 — `/vc/*` 네임스페이스 안정
- **−** Tab 전환 시 state 격리 (각 panel 독립) — 추후 Tab 1 결과를 Tab 2 입력으로 전달하는 UX 라면 별 패턴 필요

---

## 0. ADR 의사결정 그래프 (Sprint 7 carry-over)

```
ADR-022-A (facade pattern)
  ↓ vc 모듈이 master::api 의존
ADR-022-B (capacity_overflow 패키지 위치)
  ↓ vc 내부 신규 패키지 (모듈 추가 없음)
ADR-022-C (@Auditable AOP)
  ↓ KdSupplementService BR-X02 정합
ADR-022-D (PLANNER 단독 RBAC)
  ↓ REST endpoint BR-X05 보호
ADR-022-E (/vc/capacity-queue + Tabs)
  ↓ Frontend SPA 진입점
```

---

## 1. v1.0 SAD §추가 영향 (별도 패치 없음 — addendum 형식)

| § | v1.0 SAD | ADR-022 영향 |
|---|---|---|
| §3 컨테이너 매트릭스 | 9 Modulith 모듈 | 변동 없음 (ADR-022-B) |
| §4 컴포넌트 뷰 (C4 L3) | components.puml 9 모듈 | docs/architecture/modulith/ C4 diagram **재생성 후 Sprint 7 capacity_overflow 패키지 반영** (commit `18416ce`) |
| §6 데이터 아키텍처 | V001~V029 (Sprint 5 시점) | +V030~V032 (Sprint 6) + **V033** (Sprint 7 — product_priority + kd_order) |
| §7 보안 | RBAC 4 role | PLANNER 단독 REST endpoint (ADR-022-D) |
| §9.3 보안 NFR | BR-X02 audit | KdSupplementService @Auditable (ADR-022-C) |
| §10 ADR 리스트 | ADR-008~017 (Phase 2) + ADR-021 (Sprint 6) | + **ADR-022 (Sprint 7 carry-over)** |
| §11 아키텍처 리스크 | SAD-RSK-012 수주통합 지연 → BR-V12·V13 활성 공백 | ✅ 해소 (Sprint 7 carry-over 풀 스택 마감) |

---

## 2. 관련 자료

- [SAD-001 v1.0](SAD-001_Production_Scheduling_System_v1.0.md) — Phase 2 SAD (1092 line, v1.1 변경 외 그대로 유효)
- [ADR-021 Sprint 6 결정사항](ADR-021_Sprint6_Decisions_v1.0.md) — Resilience4j + audit partition + V031 + Vite chunk + Redis fanout
- [SRS v1.6](../2.SRS/SRS-001_Production_Scheduling_System_v1.6.md) — REQ-FUNC-VC-022·023 Must 승격
- [TASK-001_WBS_v1.3](../4.Tasks/TASK-001_WBS_v1.3.md) — Sprint 7 carry-over EP-22·23 활성 마감
- [Sprint-7_Completion_v1.1](../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md) — Sprint 7 carry-over 6 commit
- [Modulith C4 README v1.1](../../docs/architecture/modulith/README.md) — capacity_overflow 패키지 + ProductPriority/KdOrder facade 반영
- 백엔드 — `vc.capacity_overflow.CapacityOverflowController/Service` + `master.api.ProductPriorityLookup/KdOrderLookup`
- 프론트엔드 — `features/capacity-overflow/` + `pages/CapacityQueuePage.tsx`

---

## 3. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 7 carry-over 5 architecture decision: V033 facade pattern + capacity_overflow 패키지 위치 + @Auditable AOP + PLANNER RBAC + /vc/capacity-queue + Tabs |
