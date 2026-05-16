---
name: diagnose-spring
description: 7-step structured diagnosis for Spring Boot errors on this project. Wraps Matt Pocock's diagnose skill with Actuator + Loki + MDC traceId + BR-aware exception mapping.
---

# Diagnose Spring Boot Errors (Internal Production Scheduling)

본 skill 은 Matt Pocock 의 [diagnose](../../../../docs/harness-samples/_skills-archive/skills/engineering/diagnose/) skill 을 Spring Boot + 본 프로젝트 관측 스택 (Prometheus + Loki + Sentry + Actuator) 으로 확장합니다.

## When to use
- 500 에러 / 예외 발생 시
- 성능 회귀 (p95 > 임계)
- 모듈 경계 위반 의심 시
- BR 룰 우회 의심 시

## 7-Step Diagnosis

### 1. 재현 — Reproduce
- 단일 요청으로 재현 가능한가?
- 사용자·BR·시간대 조건 분리
- `Clock.fixed(...)` 로 시간 의존 격리

### 2. 관측 — Observe
| 도구 | 확인 |
|---|---|
| `GET /actuator/health` | UP / DOWN 어느 컴포넌트? |
| `GET /actuator/metrics/<name>` | p95 · 카운터 · 히스토그램 |
| Loki query `{app="scheduling"} \|= "traceId=<id>"` | 전체 라이프사이클 |
| Sentry | 스택 + breadcrumb + tag (`brId`, `userId`) |
| Grafana dashboard | KPI · 알림 임계 |

### 3. 격리 — Isolate
- 모듈 (`scheduling` / `audit` / `auth` / `mes` / `report`)
- 레이어 (Controller / Service / Repository / External)
- BR 식별자 (스택의 `BusinessException(BrCode.X02)` 등)

### 4. 가설 — Hypothesize
가설은 **검증 가능한 명제** 로 (예 — "BR-X07 가드가 D-2 23:59:59 경계를 놓침").

### 5. 검증 — Verify
- 단위 테스트 추가 (실패 → 성공)
- Testcontainers 통합 테스트
- Actuator metric 재측정

### 6. 수정 — Fix
- 가장 작은 범위로 수정
- `@see BR-X07` Javadoc + 테스트명 `should_enforce_BR_X07_*`
- audit log 강제 (BR-X02) — Application Service 에서 `auditLogger.log(...)` 확인

### 7. 회귀 방지 — Prevent regression
- ArchUnit 규칙으로 격상 가능한가?
- Grafana alert (Slack) 추가
- Sentry release tag 갱신

## Quick Reference — 빈출 패턴

### N+1 진단
1. `/actuator/metrics/hibernate.query.execution.count` 측정
2. Hibernate `statistics.enabled=true`
3. Loki `\|= "fetch"` 로 패턴 확인
4. 수정 — `@EntityGraph` · QueryDSL projection · `JOIN FETCH`

### Transaction propagation 오류
- `@Transactional` 메서드를 같은 클래스 내 호출 (proxy 우회) 의심
- `TransactionSynchronizationManager.isActualTransactionActive()` 로 확인

### Modulith boundary 위반
- `spring-modulith-junit` 의 `ApplicationModules.of().verify()` 실행
- `@NamedInterface` 외 직접 참조 금지

### Cache miss 폭증
- Caffeine L1 hit rate · Redis L2 hit rate `/actuator/metrics/cache.gets`
- TTL · maxSize 재검토 · cache key 정합성

### 시간대 (BR-X04) 위반
- DB `SHOW timezone;` = `Asia/Seoul`?
- Spring `application.yml` `spring.jackson.time-zone=Asia/Seoul`?
- ArchUnit `KstTimezoneArchTest` 통과?

## Anti-patterns
- 로그만 보고 가설 없이 추측
- 재현 없이 수정
- 회귀 테스트 없이 close
- BR 식별자 누락 — 어떤 룰 위반인지 명시 (PR description)

## 참고
- Matt Pocock 원본 — [docs/harness-samples/_skills-archive/skills/engineering/diagnose/](../../../../docs/harness-samples/_skills-archive/skills/engineering/diagnose/)
- Observability 설계 — `Phase 2/4.Tasks/Tasks/EP-31/`
- BR 목록 — `Phase 2/1.SRS/`
