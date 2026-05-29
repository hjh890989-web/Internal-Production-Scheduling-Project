package com.scheduling.integration;

import com.scheduling.master.api.VcMachineQuery;
import com.scheduling.vc.mes.MesPollingService;
import com.scheduling.vc.mes.MesShiftClient;
import com.scheduling.vc.mes.MesShiftEvent;
import com.scheduling.vc.mes.MesShiftEventRepository;
import com.scheduling.vc.mes.MesShiftResponse;
import com.scheduling.vc.mes.MesShiftSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 23 ST-MES-2 IT — MES polling cycle (EP-MES-ADAPTER-1, BR-X06).
 *
 * <p>{@code scheduling.mes.adapter=http} → {@link MesPollingService} + HttpMesShiftClient 활성.
 * {@link MesShiftClient} 는 @MockBean 으로 대체 (실 HTTP 는 ST-MES-3 WireMock). polling 1 cycle 시
 * 수신분만 mes_shift_event 영속 (source=MES) + 미수신 머신/shift 는 skip 검증.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MesPollingIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
        // ST-MES-2 — http adapter 활성 (MesPollingService + HttpMesShiftClient bean 로드)
        registry.add("scheduling.mes.adapter", () -> "http");
        // polling auto-trigger 비활성 (테스트는 pollOnce() 명시 호출) — 긴 initialDelay
        registry.add("scheduling.mes.poll-initial-delay-ms", () -> "3600000");
    }

    @MockitoBean private MesShiftClient mesShiftClient;

    @Autowired private MesPollingService pollingService;
    @Autowired private MesShiftEventRepository repository;
    @Autowired private VcMachineQuery machineQuery;

    @Test
    @DisplayName("polling 1 cycle — 수신 머신/shift 만 mes_shift_event 영속 (source=MES)")
    void poll_persists_only_received() {
        // 활성 머신에 LP-01 존재 전제 (seed)
        assertThat(machineQuery.findAllActive())
            .extracting(m -> m.machineId()).contains("LP-01");

        // 기본 — 모든 머신/shift 미수신 (empty)
        lenient().when(mesShiftClient.fetchShift(anyString(), any(LocalDate.class), anyShort()))
            .thenReturn(Optional.empty());
        // LP-01 shift 1 만 수신
        when(mesShiftClient.fetchShift(eq("LP-01"), any(LocalDate.class), eq((short) 1)))
            .thenReturn(Optional.of(new MesShiftResponse(
                "LP-01", LocalDate.of(2026, 5, 29), (short) 1, 144, 130, Instant.parse("2026-05-29T01:00:00Z"))));

        int persisted = pollingService.pollOnce();

        assertThat(persisted).isEqualTo(1);
        Optional<MesShiftEvent> row = repository
            .findByMachineIdAndShiftDateAndShiftNo("LP-01", LocalDate.of(2026, 5, 29), (short) 1);
        assertThat(row).isPresent();
        assertThat(row.get().getActualQty()).isEqualTo(130);
        assertThat(row.get().getSource()).isEqualTo(MesShiftSource.MES);
    }

    @Test
    @DisplayName("polling — 전 머신/shift 미수신 시 영속 0 (degraded 임계 감지에 위임)")
    void poll_persists_nothing_when_all_empty() {
        when(mesShiftClient.fetchShift(anyString(), any(LocalDate.class), anyShort()))
            .thenReturn(Optional.empty());

        assertThat(pollingService.pollOnce()).isZero();
    }
}
