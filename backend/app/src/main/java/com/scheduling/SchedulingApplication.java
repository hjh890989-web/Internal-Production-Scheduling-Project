package com.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Internal Production Scheduling — 메인 entrypoint.
 *
 * Modulith 7 모듈 합성: order · vc · ex · master · audit · notify · common.
 * 모듈 경계 검증 (@NamedInterface · 직접 호출 금지) 은 TK-00-2-2 + ArchUnit (TK-00-2-3).
 */
@SpringBootApplication
@Modulithic(
    systemName = "Internal Production Scheduling System",
    sharedModules = "common"     // common 은 모든 모듈에 자유 접근 허용
)
public class SchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }
}
