package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 명명 규칙 검증 (TK-00-2-3 §C).
 *
 * Sprint 1+ 진행하면서 Controller/Service/Repository 가 추가될 때마다
 * 본 테스트가 명명 일관성을 보장. 위반 시 BUILD FAILED.
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class NamingConventionTest {

    @ArchTest
    static final ArchRule controllers_end_with_Controller = classes()
        .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .or().areAnnotatedWith("org.springframework.stereotype.Controller")
        .should().haveSimpleNameEndingWith("Controller")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule services_end_with_Service = classes()
        .that().areAnnotatedWith("org.springframework.stereotype.Service")
        .should().haveSimpleNameEndingWith("Service")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositories_end_with_Repository = classes()
        .that().areAnnotatedWith("org.springframework.stereotype.Repository")
        .should().haveSimpleNameEndingWith("Repository")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule events_end_with_Event = classes()
        .that().resideInAPackage("..events..")
        .and().areNotAnnotations()
        .and().areTopLevelClasses()
        .and().doNotHaveSimpleName("package-info")     // package-info.java 제외
        .should().haveSimpleNameEndingWith("Event")
        .allowEmptyShould(true);

    @ArchTest
    static final ArchRule listeners_end_with_Listener = classes()
        .that().areAnnotatedWith("org.springframework.modulith.events.ApplicationModuleListener")
        .or().areAnnotatedWith("org.springframework.transaction.event.TransactionalEventListener")
        .should().haveSimpleNameEndingWith("Listener")
        .allowEmptyShould(true);
}
