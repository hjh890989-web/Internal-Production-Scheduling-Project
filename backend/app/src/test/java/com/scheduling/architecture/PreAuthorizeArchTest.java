package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * TK-30-2-2 RBAC 강제 — 모든 @RestController public method 에 @PreAuthorize 필수.
 *
 * <p>예외 (annotation 미부착 허용):
 * <ul>
 *   <li>{@code @ExceptionHandler} 메서드 — 응답 변환용</li>
 *   <li>private / static 메서드</li>
 *   <li>Spring framework 내부 메서드 (equals/hashCode/toString)</li>
 * </ul>
 *
 * <p>위반 시 빌드 FAILED — CI 게이트 (NFR-SEC-003 직접 강제).
 *
 * @see com.scheduling.security.RoleConstants
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class PreAuthorizeArchTest {

    @ArchTest
    static final ArchRule rest_controller_methods_require_preauthorize = methods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
        .and().arePublic()
        .and().areNotStatic()
        .and().areNotAnnotatedWith(ExceptionHandler.class)
        .and().haveNameNotMatching("equals|hashCode|toString")
        .should().beAnnotatedWith(PreAuthorize.class)
        .because("REQ-NF-SEC-003 — 모든 controller method 에 @PreAuthorize 강제 (RBAC 매트릭스)");
}
