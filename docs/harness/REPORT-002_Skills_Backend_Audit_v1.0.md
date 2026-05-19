# REPORT-002 skills/ 디렉토리 백엔드 적합성 감사 보고서 v1.0

**작성일**: 2026-05-16 | **버전**: 1.0 | **상태**: 사용자 검토용
**선행 문서**: REPORT-001 Harness 의사결정 권고 v1.0

> **목적**: 프로젝트 루트의 `skills/` 디렉토리에 있는 Matt Pocock 공개 skills 패키지 (총 29개) 를 본 프로젝트 backend 기술 스택 적합성 관점에서 전수 감사 + **삭제·수정·추가** 3분류 권고.

---

## 1. 요약 (1쪽 결재 양식)

| 분류 | 개수 | 비고 |
|---|:--:|---|
| **적합** (백엔드 직접 활용) | **0** | Java/Spring 특화 skill 부재 |
| **부분 적합** (간접 활용 가능) | **11** | 일반 공학 방법론 — 보존 권고 |
| **부적합** (본 프로젝트 무관) | **8** | UI·issue tracker·TS·강의 콘텐츠 — 삭제 권고 |
| **수정 권고** | **3** | Java/Spring 예시 보강 필요 |
| **추가 권고 (신규 작성)** | **9** | Backend 핵심 부재 영역 |

**6 결정 종합 1줄**:
> Backend 특화 skill 부재. 부적합 8개 archive·삭제, 부분 적합 11개 유지 (3개 backend 예시 보강), backend 필수 9개 신규 작성.

---

## 2. skills 패키지 개요

### 2.1 출처·라이선스
- **출처**: Matt Pocock 개인 공개 저장소 (커뮤니티 — Claude 공식 패키지 아님)
- **라이선스**: GPL-2.0
- **구조**: nested git 저장소 (`skills/.git/` 별도) — 본 프로젝트 외부 패키지 import 형태

### 2.2 패키지 구성

| 카테고리 | 개수 | 운영 등록 (plugin.json) | 비고 |
|---|:--:|:--:|---|
| Engineering | 10 | 10 | TDD·diagnose·architecture 등 |
| Productivity | 4 | 4 | grill-me·handoff·caveman 등 |
| Misc | 4 | 0 | git-guardrails·setup-pre-commit 등 |
| In-Progress | 4 | 0 | review·triage 등 미완 |
| Deprecated | 4 | 0 | 폐기 예정 |
| Personal | 2 | 0 | edit-article·obsidian-vault |
| **합계** | **29** | **14** | |

### 2.3 의도
- **일반 소프트웨어 공학 기초** (설계 면담·TDD·아키텍처 개선·workflow) 에 강화
- **Java/Spring backend 특화 skill 0개** — 본 프로젝트 핵심 스택 미커버

---

## 3. 백엔드 적합성 분류 (표)

### 3.1 부분 적합 — 11개 (보존)

| Skill | 카테고리 | 적합도 | 근거 |
|---|---|:--:|---|
| `diagnose` | Engineering | 부분 | 버그 재현·계측·수정 루프 — Java 디버깅 활용 가능 |
| `tdd` | Engineering | 부분 | Red-green-refactor — Spring 단위/통합 테스트 직접 적용 |
| `grill-with-docs` | Engineering | 부분 | CONTEXT.md·ADR 검증 — backend 설계에 유용 |
| `improve-codebase-architecture` | Engineering | 부분 | Module·seam·leverage — Spring Modulith 적합 |
| `triage` | Engineering | 부분 | Issue tracker 관리 — backend 업무 흐름 |
| `to-prd` | Engineering | 부분 | 대화 → PRD — 요구사항 명확화 |
| `to-issues` | Engineering | 부분 | 계획 → vertical slice — Task 분해 |
| `zoom-out` | Engineering | 부분 | 상위 추상화 설명 — 코드 네비게이션 |
| `caveman` | Productivity | 부분 | 토큰 절감 — 설계 논의 |
| `grill-me` | Productivity | 부분 | 설계 심사 — backend 아키텍처 검증 |
| `handoff` | Productivity | 부분 | 세션 인수인계 — 단독 개발 + Claude 페어 시 유용 |
| `git-guardrails-claude-code` | Misc | 부분 | Git 안전 장치 — 매우 유용 (REPORT-001 Q4 deny 정책과 시너지) |

### 3.2 부적합 — 8개 (삭제 권고)

| Skill | 카테고리 | 부적합 근거 |
|---|---|---|
| `prototype` | Engineering | UI 프로토타이핑 (React·Vue) — Frontend 영역 |
| `setup-matt-pocock-skills` | Engineering | Issue tracker (GitHub/GitLab/Backlog.md) 설정 — 단독 개발 + 자체 도구 사용 |
| `write-a-skill` | Productivity | Skill 자체 작성 도구 — meta, backend 무관 |
| `setup-pre-commit` | Misc | Node.js / Husky / Prettier / TypeScript — Java backend 의 Gradle·checkstyle·spotbugs 와 무관 |
| `migrate-to-shoehorn` | Misc | TypeScript 테스트 마이그레이션 — backend 무관 |
| `scaffold-exercises` | Misc | 강의 콘텐츠 스캐폴딩 — backend 무관 |
| `edit-article` | Personal | 문서 편집 — 개인 전용 |
| `obsidian-vault` | Personal | Obsidian 노트 — 개인 전용 |

### 3.3 부분 미완 — 4개 (수정 보류 / In-Progress·Deprecated)

| Skill | 상태 | 처리 |
|---|---|---|
| `qa` (deprecated) | Engineering | backend QA 활용 가능하나 deprecated → archive |
| `review` (in-progress) | Engineering | Standards·Spec 이중 검토 — 미완성 → 완성 시 보존 |
| (기타 in-progress 2개) | | 진행 상황에 따라 결정 |

---

## 4. 보조 디렉토리 요약

| 디렉토리 | 내용 | 본 프로젝트 영향 |
|---|---|---|
| `docs/` | ADR 1건 (`0001-explicit-setup-pointer-only-for-hard-dependencies.md`) | 정책 참고 — 직접 활용 안 함 |
| `scripts/` | `list-skills.sh`·`link-skills.sh` (symlink 자동화) | Windows 비호환 — REPORT-001 Q6 결정 (복사 방식) 과 충돌 |
| `.claude-plugin/` | `plugin.json` (14 운영 skill 등록) | **Plugin import 가능** — `npx skills@latest add mattpocock/skills` |
| `.out-of-scope/` | 3개 정책 문서 (mainstream tracker only 등) | 의도 명확화 — 정책 참고 |

---

## 5. Backend 스택 Cross-Check — 부재 영역 (9개)

본 프로젝트의 backend 필수 영역이 **Matt Pocock skills에 0개 커버**됨:

| # | 부재 영역 | 본 프로젝트 사용 처 | 영향도 |
|:--:|---|---|:---:|
| 1 | **Spring Boot Actuator + 커스텀 메트릭** | EP-31 ST-31-1, EP-40 (성능 NFR), EP-44 ST-44-5 (APM) | ⭐⭐⭐ |
| 2 | **Flyway 마이그레이션 설계** | EP-04·05·07·11 등 V1~V5 series | ⭐⭐⭐ |
| 3 | **Spring Modulith 모듈 경계** | EP-00 ST-00-2 (Modulith), EP-07 (vc.confirmed event) | ⭐⭐⭐ |
| 4 | **Spring Data JPA + QueryDSL 최적화** | 거의 모든 EP — N+1·projection·batch | ⭐⭐ |
| 5 | **Spring Security + Keycloak (JWT)** | EP-30·EP-42 | ⭐⭐⭐ |
| 6 | **Redis + Caffeine 캐시 전략** | EP-21·EP-08 등 — LISTEN/NOTIFY invalidation | ⭐⭐ |
| 7 | **ArchUnit 모듈 경계 자동 검증** | EP-00·30·34·42 등 (강제) | ⭐⭐ |
| 8 | **PostgreSQL 16 인덱스·EXPLAIN ANALYZE** | EP-44 ST-44-6 (audit 쿼리 ≤ 5s) | ⭐⭐ |
| 9 | **Docker Compose 오케스트레이션** | EP-00·30·31·32·33 (모든 인프라) | ⭐⭐⭐ |

**평가**: 본 프로젝트 핵심 영역 9개 중 **0개 커버**. Matt Pocock skills는 **일반 공학 도구**이지 **Spring 생태계 도구**가 아님.

---

## 6. 권고 (3분류)

### 6.1 삭제 권고 — 8개

**즉시 삭제 또는 archive**:

```
skills/skills/
├── prototype/                    ← 삭제
├── setup-matt-pocock-skills/     ← 삭제
├── write-a-skill/                ← 삭제 (필요 시 archive)
├── setup-pre-commit/             ← 삭제
├── migrate-to-shoehorn/          ← 삭제
└── scaffold-exercises/           ← 삭제

skills/personal/
├── edit-article/                 ← 삭제
└── obsidian-vault/               ← 삭제
```

**처리 방안**:
- (a) **Archive** — `docs/harness-samples/_skills-archive/` 로 이동 (REPORT-001 Q3 결정과 일관)
- (b) **plugin.json 등록 제거** — 14개 → 11개로 축소 (즉시 인식 안 됨, 파일은 보존)
- (c) **완전 삭제** — git history 에만 남기고 파일 제거

**추천**: **(b) plugin.json 등록 제거** — 가장 안전 + 변경량 최소. 향후 필요 시 재등록 가능.

### 6.2 수정 권고 — 3개

부분 적합 skill 중 **Java/Spring 예시 보강**이 필요한 것:

| Skill | 추가 보강 내용 |
|---|---|
| `tdd` | Testcontainers·@SpringBootTest·QueryDSL assertion·MockMvc 예시 + Phase 2 TC 매핑 (TC-VC-008 등) |
| `diagnose` | Spring Actuator endpoint·PostgreSQL EXPLAIN·Sentry stack trace·OpenTelemetry trace correlation 예시 |
| `improve-codebase-architecture` | Spring Modulith @ApplicationModuleListener·@Transactional propagation·event-driven decoupling 예시 |

**보강 방법**: SKILL.md 본문에 Java/Spring 코드 블록 추가 (2~3개 예시), 다른 도메인 예시는 유지.

### 6.3 추가 권고 — 9개 (신규 작성)

본 프로젝트 backend 필수 — **`skills/backend/`** 하위 신규 작성:

| # | 신규 Skill | 핵심 내용 | 참조 Phase 2 Epic |
|:--:|---|---|---|
| 1 | `spring-boot-actuator-design` | Prometheus endpoint, Micrometer Timer·Counter, custom metric, health check | EP-31, EP-44 |
| 2 | `flyway-migration-design` | V{N}__{name}.sql 명명, idempotent, rollback 안전, DEFERRABLE 제약 | EP-04·05·06·11·13 |
| 3 | `spring-modulith-boundaries` | 모듈 경계 정의, @ApplicationModuleListener, event 발행·구독 | EP-00 ST-00-2, EP-07·EX13 |
| 4 | `jpa-query-optimization` | N+1 감지 (Hibernate Statistics), JOIN FETCH, projection, batch_size | 전 EP |
| 5 | `spring-security-keycloak-setup` | JwtAuthenticationConverter, @PreAuthorize, OAuth2ResourceServer, role mapping | EP-30, EP-42 |
| 6 | `redis-caffeine-caching` | TTL 정책, LISTEN/NOTIFY invalidation, cache-aside, distributed lock | EP-21·EP-08 |
| 7 | `archunit-module-verification` | 패키지 의존성 규칙, @PreAuthorize 강제, Cyclical dependency 차단 | EP-00, EP-42·45 |
| 8 | `postgresql-index-explain` | B-tree·partial·GIN, EXPLAIN ANALYZE 해석, slow query 진단 | EP-44 ST-44-6 |
| 9 | `docker-compose-orchestration` | service 의존성, healthcheck, network, .env 분리, blue/green | EP-00·30·31·33 |

**각 SKILL.md 구성**:
```markdown
---
name: spring-boot-actuator-design
description: Prometheus 메트릭, 커스텀 Timer/Counter, health check 설계
---

## 언제 사용
- Spring Boot 신규 endpoint 추가 시
- Micrometer custom metric 설계
- Health check 구성

## 핵심 패턴
... (Java 예시 코드)

## 본 프로젝트 적용 참조
- EP-31 ST-31-1: Actuator 메트릭 노출
- EP-40: 성능 NFR (p95·p99 측정)
- EP-44 ST-44-5: APM 통합
```

**작업량**: skill 1건 ≈ 60~90분 × 9개 = **9~13시간**.

---

## 7. 우선순위 및 일정 추정

| 우선순위 | 작업 | 소요 | 일정 |
|:--:|---|:--:|---|
| **P0** (즉시) | 6.1 삭제 — plugin.json 등록 제거 8개 | 30분 | Stage A 시점 |
| **P1** (Sprint 0 시작 전) | 6.3 추가 — 핵심 5개 (1·2·3·5·9) | 6시간 | Sprint 0 D1~D2 |
| **P2** (Sprint 0 진행 중) | 6.2 수정 — 부분 적합 3개 보강 + 6.3 추가 나머지 4개 (4·6·7·8) | 8시간 | Sprint 0 D3~D10 |
| **P3** (Phase 3 후반) | 6.1 archive 정리 + plugin.json 최종 정리 | 1시간 | Sprint 5+ |
| **합계** | | **~15시간** | |

---

## 8. 의사결정 사항

| # | 항목 | 옵션 | 권장 |
|:--:|---|---|---|
| **R1** | skills/ 패키지를 본 프로젝트에 통합 vs 분리 보존 | (a) 통합 — `/skills/` 그대로 사용 (b) 분리 — `docs/harness-samples/` 로 이동 후 본 프로젝트 별도 `.claude/skills/` 신규 | (b) 분리 — REPORT-001 Q1·Q3 결정 일관 |
| **R2** | 부적합 8 skill 처리 | (a) 삭제 (b) plugin.json 등록만 제거 (c) archive | (b) 등록 제거 |
| **R3** | 부분 적합 3 skill 수정 권한 | (a) 직접 수정 (Matt Pocock fork) (b) `.claude/skills/` 에 별도 wrapper (c) 미수정 — 원본 유지 | (b) wrapper — 원본 보존 + 본 프로젝트 확장 |
| **R4** | 신규 9 skill 작성 위치 | (a) `skills/skills/backend/` (Matt 패키지 안) (b) `.claude/skills/` (본 프로젝트 전용) | (b) 본 프로젝트 전용 — 분리 명확 |
| **R5** | Matt Pocock skills 패키지 git submodule vs 복사 vs 제거 | (a) submodule 유지 (b) 복사 후 nested git 제거 (c) 완전 제거 | (b) 복사 후 nested 제거 — 본 프로젝트 git 단일화 |

---

## 9. 권고 종합

### 9.1 즉시 조치 (Stage A 시점, ~1시간)
1. **R5 (b)** — `skills/.git/` 제거 + 본 프로젝트 git 에 통합
2. **R1 (b)** — `skills/` 전체를 `docs/harness-samples/_skills-archive/` 로 이동
3. **R2 (b)** — `plugin.json` 등록 14 → 11 (부적합 8개 제거, 부분 적합 11개 유지, 단 6.1 권고는 plugin.json 만 수정)
4. 본 프로젝트 `.claude/skills/` 신규 디렉토리 생성

### 9.2 P1 — Sprint 0 시작 전 (~6시간)
5. **R4 (b)** — `.claude/skills/backend/` 하위 신규 5 skill 작성 (Actuator·Flyway·Modulith·Keycloak·Docker Compose)

### 9.3 P2 — Sprint 0 진행 중 (~8시간)
6. **R3 (b)** — `.claude/skills/` wrapper 로 부분 적합 3 skill 확장 (tdd-java·diagnose-spring·architecture-modulith)
7. 신규 4 skill 추가 작성 (JPA·Redis·ArchUnit·PostgreSQL)

---

## 10. 사용자 결재

### 옵션
- **결재 A**: 5 추천 모두 승인 → 즉시 Stage A 진입 (R1·R2 적용)
- **결재 B**: 일부 조정 후 승인 → 조정 사항 명시
- **결재 C**: 보류·재검토 → 추가 분석 요청 사항 명시

### 권고
**결재 A — 5 추천 모두 승인**.

본 5 결정은 trade-off 가 명확하고:
- R1·R2 는 비파괴적 (archive·등록 제거 — 복구 가능)
- R3·R4 는 본 프로젝트 분리 명확 + 원본 보존
- R5 는 git 단일화로 향후 관리 단순화

---

## 11. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-16 | (작성자) | 초안 — Matt Pocock skills 패키지 backend 적합성 감사 + 9 신규 skill 권고 |
