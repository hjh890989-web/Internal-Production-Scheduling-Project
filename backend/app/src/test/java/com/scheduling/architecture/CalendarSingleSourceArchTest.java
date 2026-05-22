package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 캘린더 단일 소스 ArchUnit — TK-07-2-1 (CON-10).
 *
 * <p>EP-06 {@code com.scheduling.master.calendar} 가 영업일 / 휴일 정의의 단일 마스터.
 * 다른 모듈 (vc / ex / order / notify / audit) 은 별도 {@code WorkingCalendar} /
 * {@code Holiday} 클래스 정의 금지. master.api.WorkingCalendar facade 만 사용 허용.
 *
 * <p>본 ArchUnit 위반 시 Modulith verify 도 함께 fail — 이중 안전망.
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class CalendarSingleSourceArchTest {

    /** {@code com.scheduling.master.calendar} 외부에 WorkingCalendar 구현 클래스 정의 금지. */
    @ArchTest
    static final ArchRule only_master_calendar_defines_working_calendar = noClasses()
        .that().resideOutsideOfPackages(
            "com.scheduling.master.calendar..",
            "com.scheduling.master.api..")
        .should().haveSimpleNameStartingWith("WorkingCalendar");

    /** {@code com.scheduling.master.calendar} 외부에 Holiday 엔티티 정의 금지. */
    @ArchTest
    static final ArchRule only_master_calendar_defines_holiday = noClasses()
        .that().resideOutsideOfPackages(
            "com.scheduling.master.calendar..")
        .should().haveSimpleNameStartingWith("Holiday")
        .orShould().haveSimpleNameStartingWith("HolidayService")
        .orShould().haveSimpleNameStartingWith("HolidayRepository");

    /** ex 모듈은 master.api.WorkingCalendar facade 만 사용 (master.calendar 직접 의존 금지). */
    @ArchTest
    static final ArchRule ex_module_uses_calendar_facade_only = classes()
        .that().resideInAPackage("com.scheduling.ex..")
        .should().onlyDependOnClassesThat()
            .resideOutsideOfPackages("com.scheduling.master.calendar..");
}
