package com.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 모듈 경계 정합성 검증 (TK-00-2-2).
 *
 * 검증 내용:
 *  - 7 모듈 (order·vc·ex·master·audit·notify·common) 모두 인식
 *  - 각 모듈의 allowedDependencies 위반 0건
 *  - 모듈 간 직접 internal 참조 0건 (NamedInterface 통한 접근만 허용)
 *  - 순환 의존 0건
 *
 * 의도적 위반 시 (예: vc.internal → order.internal 직접 import) 본 테스트가 fail.
 * 향후 ArchUnit (TK-00-2-3) 가 빌드 타임 강제 추가.
 */
class ModulithVerificationTest {

    static final ApplicationModules MODULES = ApplicationModules.of(SchedulingApplication.class);

    @Test
    void verifyModuleBoundaries() {
        MODULES.verify();
    }

    @Test
    void printModuleStructure() {
        // 콘솔 출력 — 개발 중 모듈 구조 점검 용도
        MODULES.forEach(m -> System.out.println("📦 " + m.getDisplayName()
            + "  (" + m.getName() + ")"));
    }
}
