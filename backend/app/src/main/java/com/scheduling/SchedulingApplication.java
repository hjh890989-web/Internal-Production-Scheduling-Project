package com.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Internal Production Scheduling — 메인 entrypoint.
 *
 * 7 도메인 모듈: order · vc · ex · master · audit · notify · common.
 * 1 인프라 모듈 (cross-cutting): security (TK-30-2 RBAC + JWT).
 * 모듈 경계 검증 (@NamedInterface · 직접 호출 금지) 은 TK-00-2-2 + ArchUnit (TK-00-2-3).
 */
@SpringBootApplication
@Modulithic(
    systemName = "Internal Production Scheduling System",
    sharedModules = {"common", "security"}     // 모든 모듈이 의존 가능한 횡단 인프라
)
public class SchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }
}
