package com.scheduling.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduling.vc.events.MesDegradedModeChangedEvent;
import com.scheduling.vc.mes.DegradedModeService;
import com.scheduling.vc.mes.MesPollingService;
import com.scheduling.vc.mes.MesShiftEvent;
import com.scheduling.vc.mes.MesShiftEventRepository;
import com.scheduling.vc.mes.MesShiftSource;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 23 ST-MES-4 IT — MES polling ↔ DegradedModeService 통합 (EP-MES-ADAPTER-1, BR-X06).
 *
 * <p>{@code scheduling.mes.enabled=true} + {@code adapter=http} — degraded 감지 + http polling 동시 활성.
 * <ol>
 *   <li>degraded(7h 미수신) 상태에서 정상 polling 1회 → 수신 갱신 → NORMAL 복귀</li>
 *   <li>1 shift(6h) 초과 미수신 → pollAndPublish → MesDegradedModeChangedEvent 진입 publish</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DegradedModeMesIntegrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    static {
        WIRE_MOCK.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
        registry.add("scheduling.mes.adapter", () -> "http");
        registry.add("scheduling.mes.enabled", () -> "true");   // degraded 감지 활성 (별도 flag)
        registry.add("scheduling.mes.http.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
        registry.add("scheduling.mes.http.timeout-seconds", () -> "2");
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @Autowired private MesPollingService pollingService;
    @Autowired private DegradedModeService degradedService;
    @Autowired private MesShiftEventRepository repository;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired private ApplicationEvents events;

    @BeforeEach
    void reset() {
        WIRE_MOCK.resetAll();
        circuitBreakerRegistry.circuitBreaker("mes").reset();
        repository.deleteAll();
    }

    private void seedShift(String machineId, LocalDate date, short shiftNo, Instant receivedAt) {
        repository.save(new MesShiftEvent(UUID.randomUUID(), machineId, date, shiftNo,
            144, 100, receivedAt, MesShiftSource.MES, "seed", null));
    }

    @Test
    @DisplayName("정상 polling 1회 → degraded(7h 미수신) → NORMAL 복귀")
    void polling_resolves_degraded() {
        LocalDate today = LocalDate.now();
        // 7h 전 수신 → degraded
        seedShift("LP-01", today, (short) 1, Instant.now().minus(7, ChronoUnit.HOURS));
        assertThat(degradedService.isDegraded("LP-01")).isTrue();

        // 기본 200 빈 응답 (부분응답 → empty, CB 노이즈 회피) + LP-01/shift1 만 실데이터
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift")).atPriority(10)
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{}")));
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift")).atPriority(1)
            .withQueryParam("machine", equalTo("LP-01"))
            .withQueryParam("shift_no", equalTo("1"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"machineId\":\"LP-01\",\"shiftDate\":\"" + today
                    + "\",\"shiftNo\":1,\"plannedQty\":144,\"actualQty\":140,"
                    + "\"receivedAt\":\"2026-05-29T01:00:00Z\"}")));

        int persisted = pollingService.pollOnce();

        assertThat(persisted).isGreaterThanOrEqualTo(1);
        assertThat(degradedService.isDegraded("LP-01")).isFalse();   // 수신 갱신 → 복귀
    }

    @Test
    @DisplayName("1 shift(6h) 초과 미수신 → pollAndPublish → 진입 이벤트 publish")
    void degraded_entry_publishes_event() {
        LocalDate today = LocalDate.now();
        seedShift("LP-01", today, (short) 1, Instant.now().minus(7, ChronoUnit.HOURS));

        degradedService.pollAndPublish();

        long entering = events.stream(MesDegradedModeChangedEvent.class)
            .filter(e -> e.machineId().equals("LP-01"))
            .filter(MesDegradedModeChangedEvent::isDegraded)
            .count();
        assertThat(entering).isGreaterThanOrEqualTo(1);
    }
}
