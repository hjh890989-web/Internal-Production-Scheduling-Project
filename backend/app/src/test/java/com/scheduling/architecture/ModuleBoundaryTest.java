package com.scheduling.architecture;

import com.scheduling.SchedulingApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith 모듈 경계 검증 (TK-00-2-3 §A).
 *
 * verify() — TK-00-2-2의 ApplicationModule + allowedDependencies 정의 위반 검출.
 * ArchUnit 룰 (ArchUnitArchitectureTest) 과 상호 보완:
 *   - Modulith: 런타임 정확도 (ApplicationModule annotation 기반)
 *   - ArchUnit: 빌드 강제력 (정적 분석 기반, CI/CD 게이트)
 */
class ModuleBoundaryTest {

    static final ApplicationModules MODULES = ApplicationModules.of(SchedulingApplication.class);

    @Test
    @DisplayName("Spring Modulith 모듈 경계 위반이 0건이다")
    void verifyModularStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("7 모듈이 매니페스트에 등재된다 (order·vc·ex·master·audit·notify·common)")
    void allModulesRegistered() {
        long count = MODULES.stream().count();
        assertThat(count).isEqualTo(7);
    }
}
