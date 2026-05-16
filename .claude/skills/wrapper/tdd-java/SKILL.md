---
name: tdd-java
description: Test-driven development for Java/Spring on this project. Wraps Matt Pocock's tdd skill with JUnit 5 + AssertJ + Testcontainers + ArchUnit + BR-aware test naming.
---

# TDD for Java/Spring (Internal Production Scheduling)

본 skill 은 Matt Pocock 의 [tdd](../../../../docs/harness-samples/_skills-archive/skills/engineering/tdd/) skill 을 Java/Spring 프로젝트 컨텍스트로 확장합니다.

## When to use
- 새 도메인 로직 (BR-* 룰) 구현 시
- 버그 재현 + 회귀 방지
- 리팩터 안전망 구축 시

## Test stack (이 프로젝트 표준)
- **단위** — JUnit 5 + AssertJ + Mockito
- **통합** — Testcontainers (PostgreSQL 16 + Redis 7) — DB mock 절대 금지
- **아키텍처** — ArchUnit
- **부하** — k6

## TDD Loop

### 1. Red — 실패하는 테스트 먼저
```java
@Test
void should_enforce_BR_X07_block_D2_new_entries() {
    // given
    var d2 = clock.today();
    var newEntry = ScheduleEntry.of(d2, lpMachine1);

    // when
    var ex = assertThatThrownBy(() -> service.add(newEntry));

    // then
    ex.isInstanceOf(BusinessException.class)
      .hasMessageContaining("BR-X07");
}
```

**테스트명 규칙** — `should_<expected>_BR_<id>_<context>` · BR 미적용 케이스는 `should_<expected>_when_<context>`.

### 2. Green — 최소 구현
- BR 룰만 만족시킨다. 다른 로직·추상화 추가 금지.
- `BusinessException(BrCode.X07)` throw + `@RestControllerAdvice` 가 ProblemDetail 변환.

### 3. Refactor — 통과 후 정리
- 중복 제거 (3회 등장 시)
- 추상화는 **테스트가 강제할 때만**
- Clock 주입 유지 (`Instant.now()` 금지 — ArchUnit 검증)

## Patterns 이 프로젝트 특화

### Time-dependent BR 테스트
`Clock` 주입 + `Clock.fixed(...)` 로 D-0/D-1/D-2 시나리오 격리:
```java
var clock = Clock.fixed(
    LocalDate.of(2026, 5, 16).atTime(9, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
    ZoneId.of("Asia/Seoul"));
```

### Testcontainers (통합)
```java
@Testcontainers
@SpringBootTest
class ScheduleConfirmIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

### ArchUnit BR 검증
```java
@Test
void all_BR_methods_should_have_audit_log() {
    methods().that().areAnnotatedWith(BR.class)
        .should(haveMethodCallTo(AuditLogger.class, "log"))
        .check(CLASSES);
}
```

## Anti-patterns (이 프로젝트에서 금지)
- DB mock (Mockito 로 `JpaRepository` mock) — Testcontainers 사용
- `Instant.now()`, `LocalDate.now()` — `Clock` 주입
- `@Transactional` 테스트 (실제 commit 검증 불가)
- 통합 테스트에서 `@MockBean` 으로 `@Service` 대체 — 진짜 통합 손실

## 참고
- Matt Pocock 원본 — [docs/harness-samples/_skills-archive/skills/engineering/tdd/](../../../../docs/harness-samples/_skills-archive/skills/engineering/tdd/)
- BR 목록 — `Phase 2/1.SRS/`
- ArchUnit 사례 — `Phase 2/4.Tasks/Tasks/EP-34/ST-34-3/TK-34-3-2.md`
