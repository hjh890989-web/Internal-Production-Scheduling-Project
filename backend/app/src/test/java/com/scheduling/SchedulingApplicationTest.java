package com.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — Spring Boot 컨텍스트 정상 로드 검증.
 * PG/Redis 없이 (application.yml에서 auto-config exclude) 부팅 가능 여부만 확인.
 * Sprint 1+ Task에서 with-infra profile + Testcontainers 통합 테스트 추가.
 */
@SpringBootTest
class SchedulingApplicationTest {

    @Test
    void contextLoads() {
        // 컨텍스트 로딩이 곧 검증 — 예외 없이 통과하면 OK.
    }
}
