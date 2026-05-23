# Internal Production Scheduling Project

자동차 고무 호스 제조사 사내 생산 스케줄링 시스템.

| 항목 | 값 |
|---|---|
| **Status** | **Phase 3 (개발) 완료** ✅ — Phase 4 (베타 운영) 진입 준비 (Sprint 7 carry-over 풀 스택 마감) |
| **Phase 2 산출** | 465 파일 · 253 SP · ~177 PD · 20+ ADR · SRS v1.5 |
| **Phase 3 산출** | 7 Sprint + S7 carry-over · 47 Epic · ~287 Task · **~174 commit** · 9 영업일 (AI 가속 5배 압축) |
| **언어·런타임** | Java 21 LTS · Spring Boot 3.5 · Spring Modulith 1.4 · React 18 · Vite 5 |
| **운영 규모** | 47품번 · LP 4대 + IC 1대 + 압출 4-shift × 75% (1주 horizon · ~1500 row) |
| **사용자** | ~10명 (사내 한정, 사번 8자리 + PIN 4자리 — NFR-SEC-007 v1.5) |

---

## 빠른 시작

### 백엔드 (코드 동작 확인 — Testcontainers 자동)
```bash
cd backend
./gradlew :app:test       # 249 IT (Docker Desktop 필요)
./gradlew :app:bootRun    # 부팅 (with-infra profile 시 PG/Redis 필요)
```

### 프론트엔드 (DEV server)
```bash
cd frontend
npm install
npm run dev               # http://localhost:5173 (Vite proxy → backend:8080)
npm run test:run          # vitest 54 tests
npx playwright test       # 226 E2E (STG 환경 필요)
```

### STG 베타 부팅 (사내 STG server)
```bash
cd infrastructure
cp .env.stg.example .env.stg     # vault secret 주입 (KEYCLOAK_*, KAKAO_*, VITE_AG_GRID_*)
docker compose --env-file .env.stg \
    -f docker-compose.yml -f docker-compose.stg.yml up -d
./scripts/seed-stg-beta-data.sh   # DS-VC-CONSTRAINT-47 + 1주 horizon + 9 KPI baseline
```

자세한 절차 — [docs/operations/stg-deploy.md](docs/operations/stg-deploy.md) §11 (Sprint 6 변경사항)

---

## 9 Modulith 모듈

```
com.scheduling/
  common/      BR·BrCode·ProblemDetail (의존 0)
  master/      VcConstraint·HoseRule·ExConstraint·Shift·Inventory·SettingGroup·LineType·Calendar
  order/       ExcelParser·ImportOrchestrator·FolderWatcher·Diff·Mapping
  vc/          Schedule·Rotation·Capacity·Allocator(5 룰)·Confirm·Override·Swap·events(2)
  ex/          Deadline·Yield·Demand·Grouping·Gate·Conflict·Routing·Confirm·Replan·Ranking·Export
  audit/       trigger(V025/V026/V030)·AOP(@Auditable)·Snapshot(forensic)
  notify/      WebSocket STOMP·Kakao Resilience4j·Redis fanout·ExReplanListener
  security/    Keycloak JWT·RBAC (PLANNER·STK_USER·IT_OPS·READ_ONLY)
  kpi/         BusinessKpiPersister·Controller (NS-S01~S09 + K-V01~06 + K-E01~06)
```

Modulith verify 0 위반 + ArchUnit 29 rule 통과.

---

## 9 핵심 비즈니스 룰 (hard 강제)

| BR | 의미 | 강제 layer | Sprint |
|---|---|---|---|
| **BR-V07** | 당일 (machine, slot, date) angle 단일 (일중 락) | V027 trigger + IntraDayLockRule + Override | S4 |
| **BR-E05** | `29673-2R060` reference yield = **2,531** | YieldFormula RoundingMode.HALF_UP | S3 |
| **BR-E08** | 신규 라인 우선 (NS-S09 ≥ 90%) | V024 line_type + ExLineRoutingPolicy | S4 |
| **BR-E09** | 압출 시트명 `\d+월\d+일(압출)` | ExtrusionMatrixExporter regex | S4·S6 |
| **BR-X01** | Confirmed 게이트 (DB 직접 쓰기 차단) | V022/V023 trigger + RBAC PLANNER | S4 |
| **BR-X02** | 모든 mutation audit (3년 보존 immutable) | V025 trigger + V030 partition + V026 REVOKE | S4·S6 |
| **BR-X03** | 자동 cascade (수동 호출 0건) | Modulith @ApplicationModuleListener + V031 영속 | S4·S6 |
| **BR-X04** | `Asia/Seoul` 통일 (Spring + DB + UI) | Clock 주입 + ArchUnit KstTimezoneArchTest | S0~ |
| **BR-X05** | Dual-review (작성자 ≠ 승인자) | RBAC + swap proposal accept/reject 분리 | S4 |

전체 BR 목록 — [Phase 2/2.SRS/](Phase%202/2.SRS/)

---

## Phase 3 deliverable 종합

### Sprint 0~6 (7 Sprint · 163 commit · 9 영업일)

| Sprint | 핵심 산출 | 회고 |
|---|---|---|
| **S0** 인프라·인증·CI/CD | Docker Compose + Keycloak + Prometheus baseline | [Sprint-0_Completion](Phase%203/1.Sprint-Reports/Sprint-0_Completion_v1.0.md) |
| **S1** 수주 통합 (PDD-01) | Excel parser + FolderWatcher + Diff + 매핑 검토 UI | [Sprint-1_Completion](Phase%203/1.Sprint-Reports/Sprint-1_Completion_v1.0.md) |
| **S2** 성형 가류 (PDD-02) | 47품번 vc_constraint + 5 룰 + GreedyRotationAllocator | [Sprint-2_Completion](Phase%203/1.Sprint-Reports/Sprint-2_Completion_v1.0.md) |
| **S3** 압출 (PDD-03) | D-1 역산 + yield (BR-E05=2531) + 셋팅 그룹핑 + 게이트 + 충돌 | [Sprint-3_Completion](Phase%203/1.Sprint-Reports/Sprint-3_Completion_v1.0.md) |
| **S4** 거버넌스 + 일중 락 | EP-10·11·13 Confirmed + audit + BR-V07 + Excel + cascade | [Sprint-4_Completion](Phase%203/1.Sprint-Reports/Sprint-4_Completion_v1.0.md) |
| **S5** UI 통합 | React + AG Grid + STOMP + swap + ranking + 마스터 복원 UI | [Sprint-5_Completion](Phase%203/1.Sprint-Reports/Sprint-5_Completion_v1.0.md) |
| **S6** E2E + NFR + 베타 진입 | Playwright + k6 + Resilience4j + 관측성 + 19 KPI | [Sprint-6_Completion](Phase%203/1.Sprint-Reports/Sprint-6_Completion_v1.0.md) |

종합 — [Phase-3_Completion_v1.0.md](Phase%203/2.Phase-Completion/Phase-3_Completion_v1.0.md)

---

## Phase 4 (베타 운영) 진입

진입 plan — [Phase 4/Phase-4_EntryPlan_v1.0.md](Phase%204/Phase-4_EntryPlan_v1.0.md)

운영 인계 자산:
- 배포 매뉴얼 — [docs/operations/](docs/operations/) (`stg-deploy` + `prod-deploy` + `backup-restore` + `idp-failover`)
- **베타 시나리오 SOP 5건** — [docs/operations/beta-scenarios/](docs/operations/beta-scenarios/) (BS-01~05)
- **페르소나 가이드 4건** — [docs/operations/persona/](docs/operations/persona/) (Planner / STK_USER / IT_OPS / READ_ONLY)

5 Phase 마일스톤 — STG 부팅 → 베타 시나리오 → 사용자 교육 → DR/보안 → PROD cutover.

---

## 변경 기록 (Changelog)

상세 — [CHANGELOG.md](CHANGELOG.md)

| 버전 | 날짜 | 마일스톤 |
|---|---|---|
| **v1.0.0** | 2026-05-23 | Phase 3 종료 + Phase 4 진입 준비 (Sprint 0~6 누적) |
| **v1.0.1** | 2026-05-23 | Sprint 7 carry-over 풀 스택 — BR-V12·V13 REST + Planner UI + tooling (markdownlint+cspell) |
| **v1.0.2** | 2026-05-23 | Phase 4-A 진입 직전 outward 클로저 — Controller IT (+5) + Phase-3/4 v1.1 + 4 페르소나 v1.1 + BS-06 + stg-deploy §12 |

---

## AI Harness

| 도구 | 설정 |
|---|---|
| Claude Code | [CLAUDE.md](CLAUDE.md) · [.claude/](.claude/) |
| Cursor · Gemini · Antigravity | [AGENTS.md](AGENTS.md) · [.cursor/](.cursor/) · [.gemini/](.gemini/) · [.agents/](.agents/) |
| 가이드 | [docs/harness/](docs/harness/) |

---

## Phase 1·2 산출물 탐색

- 요구사항 — [Phase 1/](Phase%201/)
- 설계 — [Phase 2/](Phase%202/) (PDD-MASTER v1.7 + SRS v1.5 + SAD v1.0 + WBS v1.2 + 20+ ADR)

---

## 라이센스 · 운영

- 사내 한정 운영 (외부 배포 없음 · 영림원 ERP 통합 범위 외)
- 단독 의사결정 + Claude 와 page-by-page 공동개발
- 문서는 새 파일 (`*_v1.x.md`) 로 버전 분리
- 코드 식별자 영문 · 콘텐츠 한국어 가능
