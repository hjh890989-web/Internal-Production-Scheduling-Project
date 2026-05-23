# Changelog

All notable changes to **Internal Production Scheduling Project** are documented in this file.

본 프로젝트는 [Semantic Versioning](https://semver.org/) 를 따릅니다 — `MAJOR.MINOR.PATCH`.
- **MAJOR**: Phase 전환 (Phase 1 → 2 → 3 → 4 → 5 → 6)
- **MINOR**: Sprint 단위 + Epic 추가
- **PATCH**: bugfix + 문서 갱신

---

## [v1.0.2] — 2026-05-23 — Phase 4-A 진입 직전 outward 문서 클로저

> v1.0.1 (Sprint 7 carry-over 풀 스택 마감) 이후 outward 자산 동기화 + REST IT 보강.
> 새 Sprint/Phase 없이 light-touch documentation + 1 quality follow-up.

### Added

- **CapacityOverflowController REST IT** — `CapacityOverflowControllerIT` 5 tests / 5 PASSED:
  - POST /split — PLANNER 200 + accepted/requestQueue JSON
  - POST /split — STK_USER 403 (BR-X05 작성자 ≠ 승인자)
  - POST /split — 미인증 401
  - POST /supplement — PLANNER 200 + 실제 KD 차감 (audit principal)
  - POST /supplement — READ_ONLY 403
  - Backend 회귀 788 → **793 tests / 0 failures**
- **베타 시나리오 BS-06** — `docs/operations/beta-scenarios/06-capacity-overflow-kd-supplement.md` (Phase 4-B 후반 후보)
  - DI-07 PRODUCT_PRIORITY + DI-08 KD_ORDER SQL 시드 예시
  - Tab1 (BR-V12) split 미리보기 + Tab2 (BR-V13) 1클릭 보충 절차
  - 기대 결과 + 실패 시 대처 5건 + KPI 영향 (Sprint 8+ Grafana panel 후보)
- **STG seed V033 옵션** — `infrastructure/scripts/seed-stg-beta-data.sh` `[5/5]` 단계:
  - `SEED_V12V13=1` 환경변수 활성 시만 PRODUCT_PRIORITY 3 + KD_ORDER 2 sample seed
  - idempotent (ON CONFLICT DO NOTHING)
- **Phase-3 Completion v1.1** + **Phase-4 EntryPlan v1.1** — Sprint 7 carry-over 풀 스택 반영, 진입 게이트 5/5 → **9/9**
- **Planner 페르소나 v1.1** + **STK_USER v1.1** + **IT_OPS v1.1** + **READ_ONLY v1.1** — 4 페르소나 모두 `/vc/capacity-queue` + BS-06 cross-reference 통일
- **stg-deploy.md §12** — Sprint 7 carry-over 변경사항 추가 (V033 + REST 2 endpoint + Frontend route + SEED_V12V13 + BS-06 + tooling + 회귀 수치)

### Metrics (v1.0.1 → v1.0.2)

| 영역 | v1.0.1 | v1.0.2 |
|---|---|---|
| Backend tests | 788 / 0 fail | **793 / 0 fail** (+5 CapacityOverflowControllerIT) |
| Frontend vitest | 58 / 0 fail | 58 / 0 fail |
| Modulith verify | 0 위반 | 0 위반 |
| ArchUnit | 29 rule | 29 rule |
| Phase 4 진입 게이트 | 9/9 | 9/9 (재확인) |
| 4 페르소나 v1.1 | 1 (Planner) | **4** (Planner + STK_USER + IT_OPS + READ_ONLY) |
| 베타 시나리오 | BS-01~05 | BS-01~05 + **BS-06 후보** |

### Commits (+7)

```text
1671157  test(vc): CapacityOverflowController REST IT — RBAC + happy path
4ceb7de  docs(phase4): EntryPlan v1.1 — Sprint 7 carry-over 풀 스택 마감 + 게이트 5→9
7b985b6  docs(phase3): Phase-3 Completion v1.1 — Sprint 7 carry-over 풀 스택 반영
1f371a6  docs(operations): Planner 페르소나 v1.1 + BS-06 신규
0ff2fe0  infra(stg): seed-stg-beta-data.sh V033 sample seed 옵션 추가 — BS-06 활성 조건
d0d6f5f  docs(operations): 3 persona v1.1 정합 — STK_USER + IT_OPS + READ_ONLY V12·V13 cross-reference
(본 commit) docs(stg): stg-deploy §12 + CHANGELOG v1.0.2
```

---

## [v1.0.1] — 2026-05-23 — Sprint 7 carry-over 풀 스택 마감

### Added

- **BR-V12·V13 REST endpoints** — `vc.capacity_overflow.CapacityOverflowController` (`POST /capacity-overflow/split`, `POST /capacity-overflow/supplement`, @PreAuthorize PLANNER)
- **BR-V12·V13 Planner UI** — `frontend/src/features/capacity-overflow/`
  - `api/capacityOverflowApi.ts` — SplitResult + SupplementResult + ConsumedEntry record 1:1
  - `CapacityOverflowSplitPanel.tsx` — daily_capa + hose qty 입력 + 자동 채택/추가 요청 큐 미리보기 + Progress 사용률
  - `KdSupplementPanel.tsx` — hose + shortage 입력 + Statistic 4종 + 소진 KD orders 테이블 (동일 hose green / 그룹 blue)
  - `pages/CapacityQueuePage.tsx` — 두 패널 Tabs 통합
  - Route `/vc/capacity-queue` + MainLayout 메뉴 + i18n ko/en (`menu.capacityQueue`)
  - `capacityOverflow.types.test.ts` — 4 단위 테스트

### Tooling

- **`.markdownlint.json`** — 11 룰 disable/relax (MD013/024/026/029/033/034/036/040/041/046 + sibling-only) — 한국어 문서 친화
- **`.cspell.json`** — 프로젝트 단어 ~100 (antd·Modulith·QueryDSL·hose·vulcanization 등) + Phase 1~5/`0.Pprompt/` 무시
- **효과** — VSCode "문제" 탭 **54 → 0**

### Reports

- `Phase 3/1.Sprint-Reports/Sprint-7_Completion_v1.1.md` — Sprint 7 풀 스택 클로저 (3 추가 commit, vitest 54→58)
- `docs/perf/PERF-002_Bundle_Regression_Report_v1.1.md` — Entry 57.41 → 57.51kB gzip (+0.10kB), `CapacityQueuePage` 2.72kB lazy chunk, antd-core +8.54kB

### Metrics (v1.0.0 → v1.0.1)

| 영역 | v1.0.0 | v1.0.1 |
|---|---|---|
| Backend tests | 788 / 0 fail | 788 / 0 fail |
| Frontend vitest | 54 / 0 fail | **58 / 0 fail** (+4) |
| Frontend lint | 0 warning | 0 warning |
| Vite entry gzip | 57.41kB | **57.51kB** (+0.10kB) |
| Vite prod build | 14.54s | 14.79s |
| Modulith verify | 0 위반 | 0 위반 |
| ArchUnit | 29 rule | 29 rule |
| VSCode 문제 탭 | 54 | **0** |

### Commits (+3)

```text
1f1313f  feat(vc): BR-V12·V13 REST endpoints — /capacity-overflow/split + /supplement
9f3f5f0  feat(ui): Sprint 7 BR-V12·V13 Planner UI — capa 큐 + KD 보충
c0df2d8  chore(tooling): VSCode 문제 탭 노이즈 차단 — .markdownlint + .cspell config
```

---

## [v1.0.0] — 2026-05-23 — **Phase 3 (개발) 완료 + Phase 4 (베타 운영) 진입 준비** 🎯

### Highlights

- **7 Sprint × 47 Epic × ~287 Task × 163 commit** (Sprint 0~6 + Phase 4 인계 자산)
- **9 영업일 (AI 가속 5배 압축)** — Phase 2 인력 가정 ~63 PD 대비
- **9 Modulith 모듈** (common·master·order·vc·ex·audit·notify·security·kpi)
- **33 Flyway 마이그레이션** V001~V032 + trigger 11종 + LISTEN/NOTIFY 7종
- **9 핵심 BR hard 강제** (X01·X02·X03·X04·V07·E05·E08·E09·X07)
- Backend **249 IT** + Frontend **54 vitest** + Playwright **226 등록** — 0 failure

### Added

#### Phase 4 인계 자산 (운영 진입 deliverable)
- `Phase 4/Phase-4_EntryPlan_v1.0.md` — STG 5주 + PROD cutover 게이트 11 KPI
- `infrastructure/scripts/seed-stg-beta-data.sh` — DS-VC-CONSTRAINT-47 + 1주 horizon + KPI baseline
- `infrastructure/.env.stg.example` — Sprint 6 신규 env var 6종 (KEYCLOAK_*, KAKAO_*, VITE_AG_GRID_*)
- `docs/operations/beta-scenarios/` — **베타 시나리오 SOP 5건** (BS-01~05)
- `docs/operations/persona/` — **페르소나 가이드 4건** (Planner/STK_USER/IT_OPS/READ_ONLY)
- `docs/operations/README.md` — 통합 index

#### Phase 3 종합 보고
- `Phase 3/2.Phase-Completion/Phase-3_Completion_v1.0.md` — Sprint 0~6 종합

### Sprint 0~6 (Phase 3 개발) — Sprint 별 상세

#### Sprint 6 (2026-05-22~23, 10 commit, 9 Epic 100%)
- **EP-E2E** — Playwright swap-cascade + master-restore + Excel CB (14 신규 tests × 2 browser)
- **EP-40** — k6 1500-row 매트릭스 부하 + PERF-001 NFR 명세 (7 NFR 매핑)
- **EP-41** — Resilience4j @Retry + @CircuitBreaker (Kakao) + spring-modulith-events-jpa 활성
- **EP-42** — Keycloak OIDC application.yml + SpEL `#{null}` default
- **EP-43** — i18n EN locale 1:1 + navigator.language 자동 감지
- **EP-44** — Prometheus scrape + Grafana 2 대시 + Loki 90일 + Promtail JSON+MDC
- **EP-45** — Playwright Excel cross-browser (Chromium + Edge)
- **EP-46** — Vite 7 chunk 세분화 (entry first paint ~50kB gzip)
- **EP-47** — 19 KPI 영속 (BusinessKpiPersister + Controller + Modulith 신규 모듈)
- 인프라 — V030 audit 월별 RANGE 파티셔닝 36 + V031 event_publication + V032 business_kpi + Redis fanout config

#### Sprint 5 (2026-05-22, 14 commit, 7 Epic 100%)
- Frontend 본격 진입 — React 18 + Vite 5 + AG Grid Enterprise + STOMP/SockJS
- **EP-15** — VcSimulationPage + 회전 격자 (BR-V04 18 회전 D1-8/N1-10)
- **EP-15 ST-15-2** — SwapProposal V028 + atomic CASE WHEN swap + DEFERRABLE UNIQUE
- **EP-16** — KakaoDeliveryService 3회 retry + V029 영속
- **EP-17** — ExMatrixPage + STOMP cascade auto-refetch + Excel 다운로드
- **EP-18** — CandidateRankingService 3 점수 (slack/balance/setting)
- **EP-19** — AuditSnapshotService forensic + MasterRestorePage UI
- **EP-20** — folder watch SLA 60초 (Sprint 0 stub 검증)

#### Sprint 4 (2026-05-22, 19 commit, 7 Epic 100%)
- **EP-10** — VC + EX Confirm 게이트 (V022·V023 trigger + RBAC)
- **EP-11** — V025 audit triggers + @Auditable AOP + V026 REVOKE + audit_reader role
- **EP-12** — Excel 역-Export (POI XSSF + BR-E09 시트명)
- **EP-13** — V027 일중 락 trigger + IntraDayLockRule + OverrideService + DO-04 영업일 키
- **EP-14** — 라인 라우팅 V024 (NEW priority + FORD fallback + ford_only)
- **EP-EX13** — PartialReplanService 정식 활성 (QUANTITY/DATE/DELETED/CONFIRMED 차단)
- **EP-EX14** — ExReplanPushListener STOMP `/topic/extrusion-updates` p95 ≤ 2초

#### Sprint 3 (2026-05-22, 20 commit, 6 Epic 100%)
- 압출 종단 파이프라인 — D-1 → yield → grouping → gate → conflict
- **BR-E05 reference** — 29673-2R060 yield = **2,531** ✅
- V017~V021 마이그레이션 — ex_schedule_candidate + shift + ex_constraint 풀확장 + inventory + setting_group
- EP-07·08·09·EX11·EX12 + EP-EX13 stub

#### Sprint 2 (2026-05-17, 18 commit, 6 Epic 100%)
- 성형 가류 — 47품번 vc_constraint + 5 룰 (LeftRight/MachinePin/HoseSlotCap/AngleCapacity/SpecLt7)
- GreedyRotationAllocator + LISTEN/NOTIFY 트리거 4종
- V009~V016 + DS-VC-CONSTRAINT-47 master_seed

#### Sprint 1 (2026-05-16, 25 commit, 5 Epic 100%)
- 수주 통합 (PDD-01) — ExcelParser + FolderWatcher + Diff + 매핑 검토 UI
- Apache POI streaming + SHA-256 중복 차단 + 60초 SLA

#### Sprint 0 (2026-05-15, 47 commit, 7 Epic 100%)
- 인프라 baseline — Docker Compose + Keycloak + Prometheus/Grafana + CI/CD (Jenkins/Harbor/SonarQube/Trivy)
- ArchUnit (NamingConvention + KstTimezone + Layered + PreAuthorize)
- V001~V008 + 마스터 데이터 진입점

### Decisions (ADR)

- **ADR-001~007** — Phase 2 PDD 정합 (자체 개발 + BPMN + WebSocket + 회전 모델 + 라우팅 + 사용자 게이트)
- **ADR-008~017** — Phase 2 SAD 정합 (Java 21 + React + PG/Redis + ApplicationEvent + Keycloak + Docker + Prometheus + Jenkins + 4-layer 일중 락 + VIEW)
- **ADR-018~020** — Phase 2 추가 (이름 명시 보류, PDD 인라인)
- **ADR-021** (2026-05-23) — Sprint 6 결정사항: Resilience4j config + audit V030 파티셔닝 전략 + event_publication public schema + Vite 7-chunk 정책

### Resolved Issues (Phase 3 누적 — 15건)

| Sprint | 이슈 | 해결 |
|---|---|---|
| S2 | JPA Schema CHAR(1) vs VARCHAR(1) | V013 ALTER VARCHAR(1) |
| S2 | NamingConvention `@Service` 이름 미일치 | @Component 변경 |
| S3 | BR-E05 spec floor vs round 불일치 | RoundingMode.HALF_UP |
| S4 | PL/pgSQL TG_OP helper 미접근 | trigger 함수 인라인 |
| S4 | Postgres P0001 → UncategorizedSQLException | DataAccessException 부모 assertion |
| S4 | V026 immutability 가 `@BeforeEach DELETE` 차단 | UUID 격리 |
| S4 | ValidateAllPerformanceIT seed BR-V07 위반 | slot 별 단일 angle |
| S5 | PostgreSQL UNIQUE 즉시 enforce | V028 DEFERRABLE + SET CONSTRAINTS DEFERRED |
| S5 | KakaoTalkClient.send false | IT expectation 보정 (3회 retry FAILED) |
| S5 | VcRotationGrid TypeScript strict | non-null assertion |
| S6 | Docker daemon 부팅 실패 (10분 × 2회) | 재설치 v4.74.0 + WSL2 정리 |
| S6 | event_publication 미생성 (ddl-auto=validate) | V031 Flyway public schema |
| S6 | RedisMessageListenerContainer "already initialized" | 수동 `afterPropertiesSet()` 제거 |
| S6 | Keycloak `${...:}` 빈 문자열 → JwtDecoder 에러 | SpEL `#{null}` default |
| S6 | jsdom navigator.language en-US (한국어 검증 실패) | `beforeAll changeLanguage('ko')` |

### Carry-over (Phase 4+ Sprint 8+)

- ~~BR-V12·V13 백엔드 + UI~~ → **v1.0.1 마감** (Sprint 7 carry-over 풀 스택)
- BR-V12 추가 요청 큐 승인 워크플로우 (Planner UI commit/reject + 백엔드 endpoint + audit) — Sprint 8+
- BR-V13 Grafana panel (IT_OPS KD remaining_qty 시각화) — Sprint 8+
- Mobile App (Flutter 압출 패드)
- ML 추천 (EP-18 ranking 자동화)
- ArchUnit DDD layer 강화 (`@DomainLayer`)
- GraphQL gateway · 사내 NAS S3 호환 · AlertManager + Slack 룰

---

## [v0.x] — Phase 1 + Phase 2 (2026-05-15 이전)

### Phase 2 (설계)
- **PDD-MASTER v1.7** (4 process — order/vc/ex/cross) + 20+ ADR
- **SRS v1.5** (NFR-SEC-007 사번 8자리 + PIN 4자리 정합)
- **SAD v1.0** + Modulith 8 모듈 + 3 schema (app/master/audit)
- **WBS v1.2** 253 SP × 47 Epic × ~290 Task 분해
- **PLAN-001 Sprint 0 EntryPlan**
- 465 파일 누적 (Phase 2 / 진행)

### Phase 1 (요구사항)
- Vision (47품번 × LP 4 + IC 1 + 압출 4-shift × 75%)
- 4 페르소나 (P1 Planner / P2 IT_OPS / P3 STK_USER / P4 READ_ONLY)
- Raw materials — 영업 Excel 4종 + 가류 마스터 + 압출 마스터

---

## Versioning Roadmap (Phase 4~6)

| 버전 | 마일스톤 | ETA |
|---|---|---|
| v1.1.0 | Phase 4-A STG 부팅 + 베타 5 시나리오 통과 | Q2 2026 |
| v1.2.0 | Phase 4-D DR 검증 + 보안 통과 | Q2 2026 |
| **v2.0.0** | **Phase 5 PROD cutover** (Blue/Green) | Q3 2026 |
| v2.x | Phase 6 carry-over (BR-V12·V13 + ML + 모바일) | Q4 2026~ |
