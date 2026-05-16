---
name: architecture-modulith
description: Architecture diagnosis + refactor for Spring Modulith boundaries on this project. Wraps Matt Pocock's improve-codebase-architecture skill with module-per-package + @NamedInterface + ArchUnit.
---

# Modulith Architecture Diagnosis (Internal Production Scheduling)

본 skill 은 Matt Pocock 의 [improve-codebase-architecture](../../../../docs/harness-samples/_skills-archive/skills/engineering/improve-codebase-architecture/) skill 을 Spring Modulith 컨텍스트로 확장합니다.

## When to use
- 모듈 간 직접 참조 의심 시
- 새 도메인 추가 시 boundary 결정
- 리팩터 전 영향 범위 파악

## 본 프로젝트 모듈 구조 (Phase 2 TK-00-2-3 정본 — 7 모듈)

> Modulith 패키지 boundary 와 DB schema 는 **1:1 매핑 아님**. 패키지 7개 (코드 의존), DB schema 3개 (데이터 권한 — `app·audit·master`, SAD §6.1.1 / PDD v1.7 ADR-010).

| 모듈 | PDD | 책임 | 주 DB schema (사용) | 외부 의존 |
|---|---|---|---|---|
| `order` | PDD-01 | 수주 정보 통합 + 변경 감지 | `app.ORDER` + `audit.ORDER_CHANGE` | Excel · MES |
| `vc` | PDD-02 | 성형 가류 (BR-V07·V12~17·X01·X07) | `app.VC_SCHEDULE` + `master.VC_*` read | Redis · Caffeine |
| `ex` | PDD-03 | 압출 (BR-E05·E08·X01·X07) | `app.EX_SCHEDULE` + `master.EX_*` read | Redis · Caffeine |
| `master` | (횡단) | 마스터 권위 (BR-X05 dual-review) | `master.*` write | — |
| `audit` | (횡단) | 감사 (BR-X02 · REQ-NF-SEC-004) | `audit.*` INSERT only | — |
| `notify` | (횡단) | WebSocket PUSH (US-04 ≤2초) | — (in-memory) | WebSocket/STOMP |
| `common` | (기반) | BR · BrCode · ProblemDetail · Clock | — | — |

## Diagnosis Loop

### 1. Boundary 확인
- `ApplicationModules.of(SchedulingApplication.class).verify()` 통과?
- ArchUnit `ModulithBoundariesArchTest` 통과?
- `common` 이 도메인 모듈 의존 0건 (역방향 의존 차단)?

### 2. 의존 그래프
```bash
./gradlew modulith:applicationModules
```
- 화살표 방향 확인 (downstream → upstream 금지)
- 순환 의존 0건 (Modulith 자동 검출)

### 3. @NamedInterface 검토
- 외부 노출은 `@NamedInterface("api")` 또는 `@NamedInterface("events")` 패키지만
- `internal/` 은 같은 모듈에서만 접근 (Phase 2 TK-00-2-3 `internal_packages_only_from_same_module`)

### 4. Event 기반 통신
- 모듈 간 동기 호출 대신 `ApplicationEventPublisher` + `@ApplicationModuleListener`
- 예 — `VcScheduleConfirmedEvent` 발행 → `audit`·`notify`·`ex` 모듈이 각각 listen

## Refactor Patterns

### 직접 참조 → Event
Before:
```java
// vc/internal/VcScheduleService.java
public class VcScheduleService {
    private final AuditLogger auditLogger;  // audit 모듈 직접 의존 ❌

    public void confirm(VcSchedule s) {
        repo.save(s);
        auditLogger.log(AuditAction.VC_CONFIRM, s.id());
    }
}
```

After:
```java
// vc/internal/VcScheduleService.java
public class VcScheduleService {
    private final ApplicationEventPublisher events;

    public void confirm(VcSchedule s) {
        repo.save(s);
        events.publishEvent(new VcScheduleConfirmedEvent(s.id()));
    }
}

// audit/internal/VcAuditEventListener.java
@ApplicationModuleListener
public void on(VcScheduleConfirmedEvent e) {
    auditLogger.log(AuditAction.VC_CONFIRM, e.scheduleId());
}
```

### Cross-module read → API + DTO
- 다른 모듈의 entity 직접 조회 금지
- `@NamedInterface` 의 read-only DTO 반환 API 호출

### Shared domain → 별도 모듈 (`common`)
- 3 모듈 이상에서 참조하는 도메인 (`BR`, `BrCode`, `ProblemDetailFactory` 등) 만 격상
- 남용 금지 (god module 회피)

## ArchUnit 표준 규칙

```java
@AnalyzeClasses(packages = "com.scheduling")
class ModulithBoundariesArchTest {

    @ArchTest
    static final ArchRule modules_should_be_acyclic =
        slices().matching("com.scheduling.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule internal_should_not_be_accessed_externally =
        noClasses().that().resideOutsideOfPackage("..internal..")
            .should().dependOnClassesThat().resideInAPackage("..internal..");

    @ArchTest
    static final ArchRule no_instant_now =
        noClasses().should().callMethod(Instant.class, "now");
}
```

## Anti-patterns
- 다른 모듈의 `@Entity` 직접 import
- `@Autowired` 로 다른 모듈의 internal service
- Cross-module 외래키 (DB 레벨 결합)
- `common` 모듈 비대화
- 동기 호출로 distributed transaction 시도 — `@TransactionalEventListener(AFTER_COMMIT)` 사용

## 참고
- Matt Pocock 원본 — [docs/harness-samples/_skills-archive/skills/engineering/improve-codebase-architecture/](../../../../docs/harness-samples/_skills-archive/skills/engineering/improve-codebase-architecture/)
- Modulith 패키지 설계 — `Phase 2/2.PDD/`
- ArchUnit 사례 — `Phase 2/4.Tasks/Tasks/EP-34/ST-34-3/TK-34-3-2.md`
