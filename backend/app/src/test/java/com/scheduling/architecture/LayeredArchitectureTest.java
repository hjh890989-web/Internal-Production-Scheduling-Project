package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Layered Architecture 검증 (TK-00-2-3 §D).
 *
 * 의존 방향: Controller → Service → Repository → Domain.
 * Event/API는 cross-cutting.
 *
 * Sprint 1+ 코드 추가 시 자동 적용. Repository가 Controller import 같은 역방향 의존은 BUILD FAILED.
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule layered_dependencies = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        // Sprint 0 시점엔 코드가 거의 없음 — 모든 layer optional. Sprint 1+ 진행하면서
        // 각 layer에 실제 클래스가 추가되면 자동 적용.
        .optionalLayer("Api").definedBy("..api..")
        .optionalLayer("Internal").definedBy("..internal..")
        .optionalLayer("Domain").definedBy("..domain..")
        .optionalLayer("Events").definedBy("..events..")

        // Api 는 외부 진입점 — 다른 layer가 의존 금지
        .whereLayer("Api").mayNotBeAccessedByAnyLayer()
        // Internal (service/handler) → Api 에서만 호출 가능
        .whereLayer("Internal").mayOnlyBeAccessedByLayers("Api")
        // Domain은 모든 layer에서 접근 가능 (값 객체·entity)
        // Events는 Internal·Api 에서만 발행 (Domain·다른 모듈은 발행 금지)
        .whereLayer("Events").mayOnlyBeAccessedByLayers("Api", "Internal");
}
