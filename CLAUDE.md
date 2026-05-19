# CLAUDE.md — Internal Production Scheduling Project

본 문서는 Claude Code 가 본 프로젝트 작업 시작 시 자동 로드하는 컨텍스트입니다.

---

## 1. 프로젝트 개요

### Vision
자동차 고무 호스 제조사의 **사내 생산 스케줄링 시스템** — 47품번 × 저압가류기(LP) 4대 + IC가류기 1대 + 압출(4-shift × 75%) 의 1주 horizon · 1500 row 일정을 자동·반복 가능하게 생성·확정·실행한다. 영림원 ERP 는 통합 범위 외 (사내 운영 중이지만 본 시스템과 분리).

### Core Features
- **스케줄 생성** — 저압가류기 슬롯 O/X, 회전수(주간 8 + 야간 10 = 18), 셋팅 그룹 1~8, 합금형 (composite 1·2·3·6) 제약 반영
- **확정 게이트 (BR-X01)** — 당일 락 (BR-V07) · 감사 강제 (BR-X02) · D-2 hard 제약 (BR-X07)
- **수율 기준 (BR-E05)** — `29673-2R060` reference yield = 2531
- **횡단 운영** — KST 통일 (BR-X04), MES 폴백 (BR-X06), dual-review (BR-X05)

### Target Audience
- **Primary** — 생산 계획 담당자 (스케줄 작성·확정), 공장장 (승인 게이트)
- **Secondary** — 압출·가류 공정 작업자 (실행 화면)

### Project Philosophy
- **단순성 우선** — 사내 한정 운영, 사용자 ~10명. SaaS·multi-tenant 패턴 회피
- **명시적 룰** — 모든 비즈니스 룰은 BR-* 식별자로 SRS·코드·테스트에 동기화 (`@BR("X01")` annotation)
- **재현 가능성** — Flyway 마이그레이션 + Testcontainers + ArchUnit 으로 환경 차이 차단

---

## 2. 기술 스택

### Backend
- **언어·런타임** — Java 21 LTS
- **프레임워크** — Spring Boot 3.3 + Spring Modulith
- **영속성** — Spring Data JPA + QueryDSL · PostgreSQL 16 · Flyway 10
- **인증** — Spring Security 6 + Keycloak 24 (SAML/OIDC) + local fallback
- **캐싱** — Redis 7 + Caffeine (2-tier)
- **실시간** — WebSocket / STOMP
- **엑셀** — Apache POI (XSSF + SXSSF streaming)
- **테스트** — JUnit 5 + Testcontainers + ArchUnit + k6 (부하)

### Frontend (Sprint 1 이후)
- **프레임워크** — React 18 + Vite + TypeScript
- **상태** — TanStack Query + Zustand
- **그리드** — AG Grid Enterprise (1500 row × 30 col)
- **시간** — dayjs + Asia/Seoul (BR-X04)

### RBAC (Phase 2 EP-30 TK-30-2-1·2)
| Keycloak role | Spring authority | 대상 |
|---|---|---|
| `PLANNER` | `ROLE_PLANNER` | 생산계획 — 작성·확정·override (BR-X05 작성자 ≠ 승인자) |
| `STK_USER` | `ROLE_STK_USER` | 현장 STK — 시뮬뷰·제안 |
| `IT_OPS` | `ROLE_IT_OPS` | IT 운영 — 마스터·Actuator·Grafana |
| `READ_ONLY` | `ROLE_READ_ONLY` | 감사·임원 — 조회 |

### Infra
- **컨테이너** — Docker Compose v2 (STG/PROD 분리) + Blue/Green (NGINX upstream toggle)
- **CI/CD** — Jenkins + Harbor + SonarQube + Trivy (ADR-015)
- **관측성** — Prometheus + Spring Actuator + Micrometer · Loki + Promtail (90일) · Grafana · Slack alert
- **APM** — OpenTelemetry + Sentry
- **백업** — pg_basebackup + WAL archiving + PITR

### 제약
- **클라우드 사용 금지** — 사내 서버 한정 (S3 호환 NAS · 사내 IdP)
- **OS 시간대 의존 금지** — `Clock` 주입 (ArchUnit 검증 — `Instant.now()` 0건)

---

## 3. 개발 가이드라인

### 비즈니스 룰 (BR-*)
모든 도메인 로직은 `BR-{Domain}{Num}` 식별자로 추적:
- **BR-V07** — 당일 (D-0) 락 · 수정 불가
- **BR-E05** — `29673-2R060` reference yield = 2531
- **BR-X01** — 확정 게이트 (D-2 ~ D-1 만 수정 가능)
- **BR-X02** — 모든 mutation audit log 강제
- **BR-X04** — 모든 timestamp `Asia/Seoul` (Spring + DB + UI 3 layer)
- **BR-X05** — Dual-review (작성자 ≠ 승인자, `ROLE_PLANNER` 내)
- **BR-X06** — MES 연동 실패 시 Excel 폴백 (1 shift 미수신 시 degraded mode)
- **BR-X07** — D-2 hard 제약 (D-2 이후 신규 추가 차단)

### Audit 보존 (REQ-NF-SEC-004)
- audit 레코드 **≥ 3년 보존** · 불변 (UPDATE/DELETE 금지)
- DB 레벨 — Audit DB 별도 schema + `REVOKE UPDATE, DELETE` + INSERT-only role
- 트리거로 변조 차단 (SAD §시스템 운영)

코드에서 BR 참조 시 — Javadoc `@see BR-X01` + 테스트명 `should_enforce_BR_X01_*`.

### 아키텍처 원칙
- **Spring Modulith** — Phase 2 TK-00-2-3 정본 **PDD 프로세스 기반 7 모듈**:
  - `com.scheduling.order` — 수주 정보 통합 (PDD-01)
  - `com.scheduling.vc` — 성형 가류 스케줄링 (PDD-02)
  - `com.scheduling.ex` — 압출 스케줄링 (PDD-03)
  - `com.scheduling.master` — 마스터 데이터 (제약·우선순위·KD)
  - `com.scheduling.audit` — 감사 (BR-X02)
  - `com.scheduling.notify` — WebSocket / 알림
  - `com.scheduling.common` — 공통 (BR·BrCode·ProblemDetail)
- **모듈 간** — `@ApplicationModuleListener` (이벤트 기반), `@NamedInterface` 외 직접 호출 금지. `common` 은 도메인 모듈 의존 금지 (ArchUnit 검증)
- **DB schema** — 데이터 의미 기반 3 schema (`app`·`audit`·`master`) — SAD §6.1.1 / PDD v1.7 ADR-010. Modulith 패키지 ≠ DB schema (1:1 매핑 아님)
- **레이어** — Controller → Service → Repository (Application Service 가 BR 강제)

### 코드 컨벤션
- **시간** — `Clock` 주입 (`Instant.now()` · `LocalDate.now()` 금지 — ArchUnit `KstTimezoneArchTest`)
- **숫자** — `BigDecimal` (가격·수율), `int` (회전수·shift)
- **로깅** — SLF4J + MDC (`traceId`, `userId`, `brId`) — Loki promtail label
- **DTO** — Java Record · 불변 · `@Valid` 강제
- **예외** — `BusinessException(BrCode)` — `@RestControllerAdvice` 에서 ProblemDetail (RFC 7807) 변환

### 테스트 표준
- **단위** — JUnit 5 + AssertJ + Mockito
- **통합** — Testcontainers (PostgreSQL 16 + Redis 7) · 실제 DB · mock 금지
- **아키텍처** — ArchUnit (모듈 경계 + `Instant.now()` 금지 + 패키지 의존)
- **부하** — k6 (1500 row · p95 < 800ms · 동시 10 사용자)

### 버전 관리
- **브랜치** — Trunk-based (main + short-lived feature/*)
- **커밋** — Conventional Commits (`feat(scheduling): ...`, `fix(audit): BR-X02 ...`)
- **PR** — 작성자 + 1 reviewer (BR-X05 적용 dual-review)
- **CI** — Jenkins (SonarQube quality gate + Trivy CVE scan + ArchUnit)

---

## 4. Subagent · Skill · Slash Command 라우팅

작업 성격에 따라 자동 위임됩니다. 수동 호출: `> use the <agent-name> subagent` 또는 `/<command>`.

### Subagents (.claude/agents/)
| 에이전트 | 사용 시점 |
|---|---|
| `java-spring` | Spring Boot 컨트롤러·서비스·리포지토리 작업 |
| `gradle` | `build.gradle.kts` (Kotlin DSL) 의존성·태스크 |
| `jpa-querydsl` | JPA + QueryDSL 동적 쿼리, 스케줄 검색 (1500 row × 30 col) |
| `spring-redis` | 2-tier 캐시 (Caffeine L1 + Redis L2), 분산락 (Redisson) |
| `react-frontend` | React + AG Grid Enterprise 프론트엔드 |

> 본 프로젝트 미사용 subagent (`kafka-pipeline`·`kafka-saga`·`flutter-app`) 는 [docs/harness-samples/_unused-agents/](docs/harness-samples/_unused-agents/) 로 아카이브.

### Skills (.claude/skills/)
| Skill | 용도 |
|---|---|
| `backend/spring-boot-actuator-design` | Actuator 메트릭 노출 + Prometheus scrape |
| `backend/flyway-migration-design` | Flyway 마이그레이션 + repeatable + baseline |
| `backend/spring-modulith-boundaries` | 모듈 경계 + `@NamedInterface` + ArchUnit |
| `backend/jpa-query-optimization` | N+1 진단 + QueryDSL projection + index hint |
| `backend/spring-security-keycloak-setup` | SAML/OIDC + local fallback + RBAC |
| `wrapper/tdd-java` | Matt Pocock TDD + Java/Spring 적용 |
| `wrapper/diagnose-spring` | Spring Boot 7-step 진단 (Actuator + 로그 + 메트릭) |
| `wrapper/architecture-modulith` | Modulith 경계 진단 + 리팩터 제안 |

### Slash Commands (.claude/commands/)
| 커맨드 | 목적 |
|---|---|
| `/fix-error` | 에러 7단계 구조화 진단 (Spring 컨텍스트) |
| `/setup-env` | Docker Compose 환경 셋업 |
| `/gitflow-commit` | Conventional Commits + Trunk-based |

---

## 5. Phase · Sprint 상태

- **Phase 1** — 요구사항 확정 (완료)
- **Phase 2** — 설계 (완료, 465 파일 · 253 SP · ~177 PD)
- **Phase 3** — 개발 (진입 준비). Sprint 0 (10 영업일 × 3 dev × 37 SP) 부터:
  - **Sprint 0** — EP-00·30·31·32·33·34·99 (인프라 + 인증 + 관측 + CI/CD + 배포 + CO)
  - **Sprint 1~5** — EP-01~20·EX·E2E + EP-40~47 (NFR)

### 참고 문서
- 요구사항 — `Phase 1/`
- 설계 — `Phase 2/` (SRS v1.4 [2.SRS] · PDD-MASTER **v1.7** [1.PDD] · SAD v1.0 [3.SAD] · WBS v1.2 · 20 ADR)
- 진입 계획 — [PLAN-001_Sprint0_Entry_Plan_v1.0.md](Phase%202/4.Tasks/PLAN-001_Sprint0_Entry_Plan_v1.0.md)
- 또한 — SRS **v1.5** (2026-05-19, NFR-SEC-007 사번+PIN 정책 재정의) `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md`
- Harness 재정립 — [PLAN-002](docs/harness/PLAN-002_AI_Harness_Reset_v1.0.md) · [REPORT-001](docs/harness/REPORT-001_Harness_Reset_Decisions_v1.0.md) · [REPORT-002](docs/harness/REPORT-002_Skills_Backend_Audit_v1.0.md) · [REPORT-003](docs/harness/REPORT-003_Harness_Phase2_Alignment_v1.0.md)

---

## 6. 운영 원칙 (사용자 협업)

- **단독 의사결정** — 경영진 결재 단계 없음. Claude 와 page-by-page 공동개발.
- **사내 서버·사내 IdP** — Keycloak 24 + 사내 LDAP/AD 동기화
- **문서 갱신** — in-place 수정 금지 · 새 파일 (`*_v1.x.md`) 로 버전 분리
- **언어** — 한국어 우선 (문서·커밋 메시지·UI). 코드 식별자는 영문.
