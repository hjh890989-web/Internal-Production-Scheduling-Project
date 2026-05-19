# Claude Code Skills (Internal Production Scheduling)

본 프로젝트 전용 skill 라이브러리. Claude Code 가 작업 컨텍스트에 따라 자동 검색·활용.

## Skills

### backend/ — Spring Boot 도메인 표준 (Sprint 0 P1)

| Skill | 용도 |
|---|---|
| [spring-boot-actuator-design](backend/spring-boot-actuator-design/SKILL.md) | Actuator + Micrometer + Prometheus + 17 KPI |
| [flyway-migration-design](backend/flyway-migration-design/SKILL.md) | Flyway schema-per-module + baseline + repeatable |
| [spring-modulith-boundaries](backend/spring-modulith-boundaries/SKILL.md) | 5 모듈 경계 + `@NamedInterface` + Event |
| [jpa-query-optimization](backend/jpa-query-optimization/SKILL.md) | N+1 + projection + index + 1500 row p95 < 800ms |
| [spring-security-keycloak-setup](backend/spring-security-keycloak-setup/SKILL.md) | SAML/OIDC + local fallback + RBAC |

### wrapper/ — Matt Pocock skill 본 프로젝트 확장

| Skill | 원본 |
|---|---|
| [tdd-java](wrapper/tdd-java/SKILL.md) | [tdd](../../docs/harness-samples/_skills-archive/skills/engineering/tdd/) — Java/Spring + Testcontainers + ArchUnit + BR |
| [diagnose-spring](wrapper/diagnose-spring/SKILL.md) | [diagnose](../../docs/harness-samples/_skills-archive/skills/engineering/diagnose/) — Spring + Actuator + Loki + Sentry |
| [architecture-modulith](wrapper/architecture-modulith/SKILL.md) | [improve-codebase-architecture](../../docs/harness-samples/_skills-archive/skills/engineering/improve-codebase-architecture/) — Modulith 5 모듈 |

## Skill 작성 표준

- frontmatter `name` + `description` 필수
- 첫 단락 — 본 프로젝트 컨텍스트로 한정
- `When to use` — 트리거 조건 명시
- 코드 예시 — Java 21 + Spring Boot 3.3 + 본 프로젝트 BR
- Anti-patterns — 명시적 금지
- 참고 — Phase 2 산출물 링크

## 신규 skill 추가 시
1. `.claude/skills/backend/<name>/SKILL.md` 작성
2. 본 README.md 표에 추가
3. CLAUDE.md §4 Skill 표에 등록
4. PR review 시 BR 정합성 + Anti-patterns 검토

## Phase 3 추가 예정 (P2 — Sprint 1+)

- redis-caffeine-caching (2-tier cache + 분산락)
- archunit-module-verification (ArchUnit 규칙 라이브러리)
- postgresql-index-explain (EXPLAIN ANALYZE 패턴)
- docker-compose-orchestration (STG/PROD + Blue/Green)

상세 — [REPORT-002](../../docs/harness/REPORT-002_Skills_Backend_Audit_v1.0.md) §6.
