package com.scheduling;

import com.scheduling.security.auth.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test — Spring Boot 컨텍스트 정상 로드 검증.
 * PG/Redis 없이 (application.yml에서 auto-config exclude) 부팅 가능 여부만 확인.
 *
 * <p>Sprint 10 EP-AUTH 도입 후 AppUserDetailsService 가 항상 등록 → JPA scan 없는
 * baseline IT 는 AppUserRepository @MockitoBean 필요.
 */
@SpringBootTest
class SchedulingApplicationTest {

    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    void contextLoads() {
        // 컨텍스트 로딩이 곧 검증 — 예외 없이 통과하면 OK.
    }
}
