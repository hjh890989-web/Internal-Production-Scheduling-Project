---
name: spring-modulith-boundaries
description: Spring Modulith 모듈 경계 설계 + @NamedInterface + ApplicationModuleListener (이 프로젝트 5 모듈 구조).
---

# Spring Modulith Boundaries

Phase 2 TK-00-2-3 정본 **PDD 프로세스 기반 7 모듈** 의 boundary 강제 표준.

## 모듈 구조 (Phase 2 정본)

```
com.scheduling/
├── SchedulingApplication.java          @SpringBootApplication
├── common/                             공용 (BR·BrCode·ProblemDetailFactory·Clock)
│   └── package-info.java               @ApplicationModule(displayName = "Common")
├── order/                              수주 정보 통합 (PDD-01)
│   ├── package-info.java               @ApplicationModule(displayName = "Order")
│   ├── api/                            @NamedInterface("api") — 외부 노출
│   ├── internal/                       모듈 내부만 (ArchUnit 강제)
│   ├── domain/                         엔터티 (ORDER · ORDER_CHANGE)
│   └── events/                         @NamedInterface("events")
│       └── OrderImportedEvent.java
├── vc/                                 성형 가류 스케줄링 (PDD-02, BR-V*)
│   ├── api/
│   ├── internal/
│   ├── domain/                         VC_SCHEDULE
│   └── events/
│       └── VcScheduleConfirmedEvent.java
├── ex/                                 압출 스케줄링 (PDD-03, BR-E*)
│   ├── api/
│   ├── internal/
│   ├── domain/                         EX_SCHEDULE
│   └── events/
│       └── ExScheduleConfirmedEvent.java
├── master/                             마스터 데이터 (제약·우선순위·KD)
│   ├── api/
│   ├── internal/
│   └── domain/                         VC_CONSTRAINT · EX_CONSTRAINT · VC_HOSE_RULE · PRODUCT_PRIORITY · KD_ORDER
├── audit/                              감사 (BR-X02, REQ-NF-SEC-004)
│   ├── api/
│   ├── internal/
│   └── domain/                         VC_AUDIT · EX_AUDIT · ORDER_CHANGE
└── notify/                             WebSocket / 알림 (US-04 PUSH ≤2초)
    ├── api/
    └── internal/
```

## 모듈 별 책임 (Phase 2 PDD 매핑)

| 모듈 (패키지) | PDD | 책임 | 주 사용 DB schema |
|---|---|---|---|
| `order` | PDD-01 | 수주 정보 통합 + 변경 감지 | `app.ORDER` + `audit.ORDER_CHANGE` |
| `vc` | PDD-02 | 성형 가류 스케줄링 (BR-V07·V12~17·X01·X07) | `app.VC_SCHEDULE` + `master.VC_*` (read) |
| `ex` | PDD-03 | 압출 스케줄링 (BR-E05·E08·X01·X07) | `app.EX_SCHEDULE` + `master.EX_*` (read) |
| `master` | (횡단) | 마스터 데이터 권위 (BR-X05 dual-review) | `master.*` (write — `master_admin`) |
| `audit` | (횡단) | 감사 로그 (BR-X02·REQ-NF-SEC-004) | `audit.*` (INSERT only) |
| `notify` | (횡단) | WebSocket PUSH (US-04 ≤2초, ADR-003) | — (in-memory 세션) |
| `common` | (기반) | BR · BrCode · ProblemDetail · Clock | — |

## @ApplicationModule 선언

```java
// vc/package-info.java (성형 스케줄링)
@org.springframework.modulith.ApplicationModule(
    displayName = "Vulcanization Scheduling",
    allowedDependencies = {"common", "master::api", "order::events", "audit::events"}
)
package com.scheduling.vc;
```

- `allowedDependencies` — 의존 가능한 모듈 명시
- `master::api` — `master` 모듈의 `api` named interface 만 (제약 read)
- `order::events` — `order` 모듈의 이벤트 (수주 통합 후 트리거)

## @NamedInterface

```java
// vc/api/package-info.java
@org.springframework.modulith.NamedInterface("api")
package com.scheduling.vc.api;
```

- 다른 모듈은 `api` 패키지만 import 가능
- `internal/` 은 모듈 내에서만 접근 (ArchUnit `internal_packages_only_from_same_module` 강제)

## 모듈 간 통신 — Event 기반

### 발행 (Publisher) — `vc` 모듈
```java
@Service
@RequiredArgsConstructor
public class VcScheduleService {

    private final ApplicationEventPublisher events;
    private final VcScheduleRepository repo;
    private final Clock clock;

    @Transactional
    public void confirm(UUID id, String userId) {
        var schedule = repo.findById(id).orElseThrow();
        schedule.confirm(userId, clock.instant());
        repo.save(schedule);

        // 이벤트만 발행 — audit · ex · notify 가 각각 listen
        events.publishEvent(new VcScheduleConfirmedEvent(id, userId, clock.instant()));
    }
}
```

### 수신 (Listener) — `audit` 모듈
```java
@Component
@RequiredArgsConstructor
public class VcAuditEventListener {

    private final AuditLogger logger;

    @ApplicationModuleListener  // @Async + @TransactionalEventListener(AFTER_COMMIT) 동등
    void on(VcScheduleConfirmedEvent e) {
        logger.log(AuditAction.VC_CONFIRM, e.scheduleId(), e.userId());
    }
}
```

### 수신 — `notify` 모듈 (US-04 PUSH ≤ 2초)
```java
@Component
@RequiredArgsConstructor
public class VcConfirmNotifier {

    private final SimpMessagingTemplate websocket;

    @ApplicationModuleListener
    void on(VcScheduleConfirmedEvent e) {
        websocket.convertAndSend("/topic/vc-confirmed", e);  // SimView 패드 PUSH
    }
}
```

**중요** — `@ApplicationModuleListener` 는 commit 후 비동기 처리. transactional outbox 의 단순 버전.

## Verify Boundaries

### ApplicationModulesIntegrationTest

```java
@SpringBootTest
class ApplicationModulesTest {

    @Test
    void verify_boundaries() {
        var modules = ApplicationModules.of(Application.class);
        modules.verify();           // 위반 시 실패
        modules.forEach(System.out::println);  // 문서화
    }

    @Test
    void document_modules() {
        new Documenter(ApplicationModules.of(Application.class))
            .writeDocumentation()    // target/modulith-docs/
            .writeIndividualModulesAsPlantUml();
    }
}
```

### ArchUnit 보강 (Phase 2 TK-00-2-3 정본)

```java
@AnalyzeClasses(packages = "com.scheduling")
class ModulithArchTest {

    @ArchTest
    static final ArchRule modules_are_acyclic =
        slices().matching("com.scheduling.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule internal_packages_only_from_same_module =
        noClasses().that().resideOutsideOfPackage("com.scheduling.(*)..")
            .or().resideOutsideOfPackages("com.scheduling.common..", "com.scheduling.(*).internal..")
            .should().accessClassesThat().resideInAPackage("..internal..");

    @ArchTest
    static final ArchRule common_does_not_depend_on_domain_modules =
        noClasses().that().resideInAPackage("com.scheduling.common..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.scheduling.order..",
                "com.scheduling.vc..",
                "com.scheduling.ex..",
                "com.scheduling.master..",
                "com.scheduling.audit..",
                "com.scheduling.notify.."
            );
}
```

## DB 결합 차단 (코드 계층)

> **중요** — Modulith 패키지 boundary 와 DB schema 는 **1:1 매핑 아님**. DB schema 는 데이터 의미 기반 3 분리 (`app`·`audit`·`master` — SAD §6.1.1 / PDD v1.7 ADR-010). 본 섹션은 **코드 계층 boundary** 만 다룸.

- 다른 모듈의 `@Entity` 직접 import 금지 — `@NamedInterface` 의 read-only DTO API 호출
- 다른 모듈의 `JpaRepository` 직접 의존 금지 (ArchUnit 검증)
- Cross-module read 는 모듈 API (`api/` named interface) 경유
- 모듈 내부에서 cross-schema join 은 허용 (예: `vc` 모듈이 `master.VC_CONSTRAINT` read — 도메인 의미 결합) — 단 다른 모듈의 entity 직접 의존은 금지

## Anti-patterns
- 다른 모듈의 `@Entity` 직접 import
- 다른 모듈의 `JpaRepository` 직접 의존
- `@Autowired` 로 다른 모듈의 `internal/` 서비스
- 동기 호출로 모듈 간 트랜잭션 (`@Transactional` propagation)
- DB schema 를 Modulith 모듈 별로 분리 (잘못된 가정 — 데이터 의미 기반 분리가 정본)
- `common` 모듈에 도메인 로직 (god module)
- Event payload 에 entity (mutable 노출) — DTO 또는 ID 만 전달

## 새 모듈 추가 절차

1. `com.scheduling.<newmodule>/package-info.java` 에 `@ApplicationModule`
2. `api/`, `internal/`, `events/` 패키지 + `@NamedInterface`
3. 새 모듈의 entity 는 적절한 schema 에 — 운영 데이터 = `app`, 감사 = `audit`, 마스터 = `master` (Modulith 모듈 별 schema 신설 금지)
4. `ApplicationModulesTest.verify_boundaries()` 통과
5. ArchUnit 통과
6. 다른 모듈은 event listen 만 허용 — 직접 호출 PR review 차단

## 참고
- Phase 2 PDD v1.7 ADR-009 (Modulith 1.2 채택) + ADR-010 (Schema 분리)
- Phase 2 SAD §6.1.1 (DB schema 정본 — `app·audit·master`)
- Phase 2 EP-34 ST-34-3 (KST + ArchUnit) — `Phase 2/4.Tasks/Tasks/EP-34/ST-34-3/`
- Spring Modulith 공식 문서 (Boot 3.3 + Modulith 1.2)
