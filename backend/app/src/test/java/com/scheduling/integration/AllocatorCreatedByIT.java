package com.scheduling.integration;

import com.scheduling.master.api.SlotCompatibilityQuery;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.yield.VcYieldCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 17 EP-DAY-LOCK TK-DAY-LOCK-2-4 — Allocator generated VcSchedule.createdBy 회귀.
 *
 * <p>BR-X05 dual-review actor 책임 — AllocationContext.requestedBy 가 Allocator 출력
 * VcSchedule.createdBy 로 전파되는지 검증.
 *
 * <p>검증:
 * <ul>
 *   <li>requestedBy="00000001" → 모든 schedule createdBy="00000001"</li>
 *   <li>4-arg 생성자 (legacy) → 모든 schedule createdBy="system" default</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class AllocatorCreatedByIT {

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

    @Autowired private GreedyRotationAllocator allocator;
    @Autowired private CapacityLedgerBuilder ledgerBuilder;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private SlotCompatibilityQuery compatQuery;
    @Autowired private VcYieldCalculator yieldCalc;

    private static final LocalDate MON = LocalDate.of(2026, 2, 16);
    private static final LocalDate DELIVERY = LocalDate.of(2026, 3, 2);

    @BeforeEach
    void rebuildCaches() {
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private AllocationContext context(String requestedBy) {
        Map<String, Integer> qRequired = new HashMap<>();
        Map<String, List<OrderInput>> ordersByHose = new HashMap<>();
        String hose = "29673-2R060";    // BR-E05 reference yield 2531
        qRequired.put(hose, 5_000);
        ordersByHose.put(hose, List.of(new OrderInput(UUID.randomUUID(), hose, DELIVERY, 5_000)));
        List<LocalDate> workingDays = List.of(MON, MON.plusDays(1), MON.plusDays(2),
            MON.plusDays(3), MON.plusDays(4));
        CapacityLedger ledger = ledgerBuilder.build(workingDays.get(0), workingDays.get(4));
        return new AllocationContext(qRequired, ordersByHose, ledger, workingDays, requestedBy);
    }

    @Test
    @DisplayName("BR-X05 — AllocationContext.requestedBy → 모든 schedule createdBy 전파")
    void allocator_propagates_requested_by() {
        AllocationResult result = allocator.allocate(context("00000001"));
        assertThat(result.schedules()).isNotEmpty();
        for (VcSchedule s : result.schedules()) {
            assertThat(s.getCreatedBy())
                .as("schedule %s createdBy", s.getVcScheduleId())
                .isEqualTo("00000001");
        }
    }

    @Test
    @DisplayName("legacy 4-arg AllocationContext → createdBy 'system' default")
    void allocator_legacy_constructor_uses_system_default() {
        Map<String, Integer> qRequired = new HashMap<>();
        Map<String, List<OrderInput>> ordersByHose = new HashMap<>();
        String hose = "29673-2R060";
        qRequired.put(hose, 2_500);
        ordersByHose.put(hose, List.of(new OrderInput(UUID.randomUUID(), hose, DELIVERY, 2_500)));
        List<LocalDate> workingDays = List.of(MON, MON.plusDays(1), MON.plusDays(2),
            MON.plusDays(3), MON.plusDays(4));
        CapacityLedger ledger = ledgerBuilder.build(workingDays.get(0), workingDays.get(4));
        AllocationContext legacy = new AllocationContext(qRequired, ordersByHose, ledger, workingDays);

        AllocationResult result = allocator.allocate(legacy);
        assertThat(result.schedules()).isNotEmpty();
        for (VcSchedule s : result.schedules()) {
            assertThat(s.getCreatedBy()).isEqualTo("system");
        }
    }
}
