package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * KST 시간 통일 (BR-X04) 강제 — TK-34-3-2.
 *
 * <p>정적 {@code *.now()} 호출 금지 — JVM 의 시스템 시계에 직접 의존하면 테스트 격리·시간대 검증 불가.
 * 도메인 코드는 {@link java.time.Clock} 주입 받아 {@code Instant.now(clock)} 형태로 사용.
 *
 * <p>본 규칙은 production 코드 ({@code main/} 소스 트리) 에만 적용. 테스트 코드는 자유.
 *
 * @see com.scheduling.config.TimeZoneConfig#clock()
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class KstTimezoneArchTest {

    @ArchTest
    static final ArchRule no_static_instant_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(Instant.class, "now")
        .because("Clock 주입 사용 — Instant.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_static_localdate_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(LocalDate.class, "now")
        .because("Clock 주입 사용 — LocalDate.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_static_localdatetime_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(LocalDateTime.class, "now")
        .because("Clock 주입 사용 — LocalDateTime.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_static_localtime_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(LocalTime.class, "now")
        .because("Clock 주입 사용 — LocalTime.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_static_zoneddatetime_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(ZonedDateTime.class, "now")
        .because("Clock 주입 사용 — ZonedDateTime.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_static_offsetdatetime_now = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(OffsetDateTime.class, "now")
        .because("Clock 주입 사용 — OffsetDateTime.now(clock) (BR-X04)");

    @ArchTest
    static final ArchRule no_legacy_date_default_constructor = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callConstructor(Date.class)
        .because("java.util.Date 금지 — java.time API + Clock 주입 사용 (BR-X04)");

    @ArchTest
    static final ArchRule no_system_currenttimemillis = noClasses()
        .that().resideInAPackage("com.scheduling..")
        .should().callMethod(System.class, "currentTimeMillis")
        .because("Clock 주입 사용 — clock.millis() (BR-X04)");

    // 참고: System.nanoTime() 은 단조 시간 측정 — BR-X04 (wall-clock KST) 와 무관.
    //       performance timing (elapsed ms 측정) 용도로 허용.
}
