# AGENTS.md — Internal Production Scheduling Project

본 문서는 cross-tool 공통 컨텍스트 (Cursor · Gemini · Antigravity · Claude Code 동시 인식). 도구별 상세는 `.claude/`, `.cursor/`, `.gemini/`, `.agents/` 참조. Claude Code 전용 상세는 [CLAUDE.md](CLAUDE.md).

---

## 1. Project Overview

**Vision** — 자동차 고무 호스 제조사 사내 생산 스케줄링 시스템. 47품번 × LP가류기 4대 + IC가류기 1대 + 압출(4-shift × 75%) 의 1주 horizon · 1500 row 일정을 자동·반복 가능하게 생성·확정·실행. 영림원 ERP 통합 범위 외.

**Core Features**
- 스케줄 자동 생성 (저압가류기 슬롯 O/X · 회전수 18 · 셋팅 그룹 1~8 · 합금형 1·2·3·6)
- 확정 게이트 (BR-X01) · 당일 락 (BR-V07) · D-2 hard 제약 (BR-X07)
- 수율 기준 (BR-E05 — `29673-2R060` reference = 2531)
- 횡단 운영 — KST 통일 (BR-X04) · MES 폴백 (BR-X06) · dual-review (BR-X05)

**Project Goals**
- 스케줄 생성 시간 — 1500 row p95 < 800ms
- 확정 → 실행 신뢰성 — audit 100% (BR-X02)
- 가동률 — LP 가류기 95%+ · IC 가류기 90%+

---

## 2. Technical Stack

**Backend Core**
- Java 21 LTS · Spring Boot 3.3 · Spring Modulith
- Spring Data JPA + QueryDSL · PostgreSQL 16 · Flyway 10
- Spring Security 6 + Keycloak 24 (SAML/OIDC + local fallback)
- Redis 7 + Caffeine (2-tier) · WebSocket/STOMP · Apache POI

**Frontend (Sprint 1+)**
- React 18 + Vite + TypeScript · TanStack Query + Zustand
- AG Grid Enterprise (1500 row × 30 col) · dayjs Asia/Seoul

**Infra**
- Docker Compose v2 (STG/PROD 분리) + Blue/Green
- Jenkins + Harbor + SonarQube + Trivy (ADR-015)
- Prometheus + Loki + Grafana · OpenTelemetry + Sentry
- pg_basebackup + WAL archiving + PITR

**Test**
- JUnit 5 · Testcontainers · ArchUnit · k6

**Constraints**
- 클라우드 사용 금지 — 사내 서버 한정 (S3 호환 NAS · 사내 IdP)
- OS 시간대 의존 금지 — `Clock` 주입 강제 (ArchUnit)

---

## 3. Development Guidelines

**Business Rules** — 모든 도메인 로직은 `BR-{Domain}{Num}` 식별자 추적 (BR-V07·E05·X01·X02·X04·X05·X06·X07). 코드 `@see BR-X01` + 테스트 `should_enforce_BR_X01_*`. Audit 보존 — REQ-NF-SEC-004 ≥ 3년 + INSERT-only (REVOKE UPDATE/DELETE).

**Architecture**
- Spring Modulith 7 모듈 (Phase 2 TK-00-2-3 정본, PDD 프로세스 기반): `order`·`vc`·`ex`·`master`·`audit`·`notify`·`common`
- 모듈 간 — `@ApplicationModuleListener` 이벤트 · `@NamedInterface` 외 직접 호출 금지 · `common` 은 도메인 모듈 의존 금지
- DB 의미 기반 3 schema (`app`·`audit`·`master`) — SAD §6.1.1 / PDD v1.7 ADR-010. Modulith 패키지 ≠ DB schema (1:1 매핑 아님). audit schema `REVOKE UPDATE, DELETE` (BR-X02·REQ-NF-SEC-004)
- Controller → Service → Repository · Application Service 가 BR 강제

**Code Style**
- 시간 — `Clock` 주입 (ArchUnit 금지 검증)
- 숫자 — `BigDecimal` (가격·수율) / `int` (회전수·shift)
- 로깅 — SLF4J + MDC (`traceId`, `userId`, `brId`)
- DTO — Java Record + `@Valid`
- 예외 — `BusinessException(BrCode)` → ProblemDetail (RFC 7807)

**Test**
- 통합 테스트는 Testcontainers (실제 DB) · DB mock 금지
- ArchUnit 으로 모듈 경계 + `Instant.now()` 금지 검증
- 부하 — k6 (1500 row · p95 < 800ms · 동시 10 사용자)

**Performance**
- 스케줄 조회 (1500 row × 30 col) — p95 < 800ms
- 확정 트랜잭션 — p95 < 200ms
- Excel export — 1500 row < 5s
- Actuator `/health` — < 100ms

**Version Control**
- Trunk-based (main + short-lived feature/*)
- Conventional Commits (`feat(scheduling): ...`)
- PR — 작성자 + 1 reviewer (dual-review, BR-X05)
- CI — Jenkins (SonarQube + Trivy + ArchUnit)

---

## 4. Tool-Specific Configurations

| 도구 | 설정 디렉토리 | 비고 |
|---|---|---|
| Claude Code | [.claude/](.claude/) | agents · commands · skills · settings.local.json |
| Cursor | [.cursor/](.cursor/) | rules · skills · agents |
| Gemini | [.gemini/](.gemini/) | agents |
| Common | [.agents/](.agents/) | rules · workflows · skills |

도구 별 상세는 [docs/harness/](docs/harness/) 참조.

---

## 5. 운영 원칙

- 사내 한정 운영 · 사용자 ~10명 (SaaS 패턴 회피)
- 단독 의사결정 + Claude 와 page-by-page 공동개발
- 문서 갱신은 새 파일 (`*_v1.x.md`) · in-place 수정 금지
- 한국어 우선 (문서·커밋·UI) · 영문 코드 식별자
