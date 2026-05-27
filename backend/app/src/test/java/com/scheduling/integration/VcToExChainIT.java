package com.scheduling.integration;

import com.scheduling.ex.event.ExtrusionScheduleService;
import com.scheduling.ex.event.PartialReplanService;
import com.scheduling.vc.events.VcChangedEvent;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Sprint 15 EP-EX-FULL ST-EX-1 — VC → EX chain IT (TK-EX-1-1·2).
 *
 * <p>Sprint 6 EP-EX13/14 의 VcConfirmedListener + VcChangedListener 가 실제 호출되는지 검증:
 * <ul>
 *   <li>VcConfirmedEvent → VcConfirmedListener → ExtrusionScheduleService.generateCandidates</li>
 *   <li>VcChangedEvent → VcChangedListener → PartialReplanService.replanWithContext</li>
 * </ul>
 *
 * <p>@ApplicationModuleListener AFTER_COMMIT — TransactionTemplate 으로 publish 필수 (Sprint 14 교훈).
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcToExChainIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @MockitoBean private ExtrusionScheduleService extrusionService;
    @MockitoBean private PartialReplanService replanService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    @DisplayName("TK-EX-1-1 VcConfirmedEvent → ExtrusionScheduleService.generateCandidates (Sprint 6 EP-07 chain)")
    void vc_confirmed_triggers_ex_candidates() {
        VcConfirmedEvent event = new VcConfirmedEvent(
            UUID.randomUUID(),
            Instant.now(),
            List.of(new VcConfirmedEvent.VcConfirmedRow(
                UUID.randomUUID(), "29673-2R060", LocalDate.of(2026, 6, 1),
                "LP-01", (short) 1, (short) 1, 2531))
        );

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
        });

        verify(extrusionService, timeout(5_000)).generateCandidates(any(VcConfirmedEvent.class));
    }

    @Test
    @DisplayName("TK-EX-1-2 VcChangedEvent → PartialReplanService.replanWithContext (Sprint 6 EP-EX13 chain, BR-X03)")
    void vc_changed_triggers_partial_replan() {
        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(),
            Instant.now(),
            List.of(new VcChangedEvent.VcChangedRow(
                UUID.randomUUID(), "29673-2R030",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2),
                100, 130, VcChangedEvent.ChangeType.QUANTITY))
        );

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
        });

        verify(replanService, timeout(5_000)).replanWithContext(any(VcChangedEvent.class));
    }
}
