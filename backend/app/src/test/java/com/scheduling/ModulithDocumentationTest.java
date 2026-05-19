package com.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Modulith 모듈 다이어그램 자동 생성 (PlantUML + AsciiDoc 매니페스트).
 * 산출물: app/build/spring-modulith-docs/
 *   - components.puml             — 전체 모듈 다이어그램
 *   - module-<name>.puml          — 개별 모듈 다이어그램
 *   - module-<name>.adoc          — AsciiDoc 모듈 명세
 *
 * SAD §4 다이어그램과 1:1 매핑 확인 가능. CI/CD 산출물로 보존.
 */
class ModulithDocumentationTest {

    static final ApplicationModules MODULES = ApplicationModules.of(SchedulingApplication.class);

    @Test
    void writeDocumentation() {
        new Documenter(MODULES)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases();
    }
}
