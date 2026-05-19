package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit 모듈 아키텍처 규칙 (TK-00-2-3 §B).
 *
 * 정적 분석 기반 — 빌드 타임에 위반 차단. Spring Modulith verify 와 상호 보완.
 * 본 테스트 통과가 Sprint 0 DoD 항목 2 ("모듈 경계 위반 0") 직접 달성.
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchUnitArchitectureTest {

    // ---------- 1. 모듈 간 의존 순환 금지 ----------
    @ArchTest
    static final ArchRule no_module_cycles = slices()
        .matching("com.scheduling.(*)..")
        .should().beFreeOfCycles();

    // ---------- 2. internal 패키지는 같은 모듈 내부에서만 접근 ----------
    @ArchTest
    static final ArchRule internal_packages_only_from_same_module = noClasses()
        .that().resideOutsideOfPackages(
            "com.scheduling.order.internal..",
            "com.scheduling.vc.internal..",
            "com.scheduling.ex.internal..",
            "com.scheduling.master.internal..",
            "com.scheduling.audit.internal..",
            "com.scheduling.notify.internal.."
        )
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "com.scheduling.order.internal..",
            "com.scheduling.vc.internal..",
            "com.scheduling.ex.internal..",
            "com.scheduling.master.internal..",
            "com.scheduling.audit.internal..",
            "com.scheduling.notify.internal.."
        )
        .allowEmptyShould(true);

    // ---------- 3. common 모듈은 도메인 모듈에 의존 금지 (순환 방지 — SAD §4.1) ----------
    @ArchTest
    static final ArchRule common_does_not_depend_on_domain_modules = noClasses()
        .that().resideInAPackage("com.scheduling.common..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "com.scheduling.order..",
            "com.scheduling.vc..",
            "com.scheduling.ex..",
            "com.scheduling.master..",
            "com.scheduling.audit..",
            "com.scheduling.notify.."
        )
        .allowEmptyShould(true);

    // ---------- 4. domain 패키지는 같은 모듈 내부에서만 접근 ----------
    @ArchTest
    static final ArchRule domain_packages_only_from_same_module = noClasses()
        .that().resideOutsideOfPackages(
            "com.scheduling.order.domain..",
            "com.scheduling.vc.domain..",
            "com.scheduling.ex.domain..",
            "com.scheduling.master.domain..",
            "com.scheduling.audit.domain..",
            "com.scheduling.notify.domain.."
        )
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "com.scheduling.order.domain..",
            "com.scheduling.vc.domain..",
            "com.scheduling.ex.domain..",
            "com.scheduling.master.domain..",
            "com.scheduling.audit.domain..",
            "com.scheduling.notify.domain.."
        )
        .allowEmptyShould(true);
}
