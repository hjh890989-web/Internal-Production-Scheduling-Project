package com.scheduling.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduling.vc.mes.MesShiftClient;
import com.scheduling.vc.mes.MesShiftResponse;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 23 ST-MES-3 IT — HttpMesShiftClient 실 HTTP 4 시나리오 (EP-MES-ADAPTER-1).
 *
 * <p>WireMock 실 HTTP 서버로 {@link com.scheduling.vc.mes.HttpMesShiftClient} 검증:
 * <ol>
 *   <li>정상 200 → MesShiftResponse 파싱 + present</li>
 *   <li>5xx 연속 → @Retry 3회 → fallback empty</li>
 *   <li>timeout (응답 지연 > timeout) → @Retry → fallback empty</li>
 *   <li>부분 응답 (machineId 누락) → isComplete=false → empty</li>
 * </ol>
 *
 * <p>CircuitBreaker "mes" 는 @BeforeEach 마다 reset — 메서드 간 상태 격리.
 * 영속(reportProduction) 경로는 ST-MES-2 MesPollingIT 가 별도 검증.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WireMockMesIT {

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
        // ST-MES-3 — http adapter + WireMock base-url + 짧은 timeout (시나리오 3 가속)
        registry.add("scheduling.mes.adapter", () -> "http");
        registry.add("scheduling.mes.http.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
        registry.add("scheduling.mes.http.timeout-seconds", () -> "1");
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @Autowired private MesShiftClient client;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

    private static final LocalDate DATE = LocalDate.of(2026, 5, 29);

    @BeforeEach
    void reset() {
        WIRE_MOCK.resetAll();
        circuitBreakerRegistry.circuitBreaker("mes").reset();   // 메서드 간 circuit 상태 격리
    }

    @Test
    @DisplayName("시나리오1 정상 200 — MesShiftResponse 파싱 + present")
    void scenario1_ok() {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {"machineId":"LP-01","shiftDate":"2026-05-29","shiftNo":1,
                 "plannedQty":144,"actualQty":130,"receivedAt":"2026-05-29T01:00:00Z"}""")));

        Optional<MesShiftResponse> result = client.fetchShift("LP-01", DATE, (short) 1);

        assertThat(result).isPresent();
        assertThat(result.get().machineId()).isEqualTo("LP-01");
        assertThat(result.get().actualQty()).isEqualTo(130);
        assertThat(result.get().isComplete()).isTrue();
    }

    @Test
    @DisplayName("시나리오2 5xx 연속 — @Retry 3회 → fallback empty")
    void scenario2_server_error_retries_then_fallback() {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift"))
            .willReturn(aResponse().withStatus(503)));

        Optional<MesShiftResponse> result = client.fetchShift("LP-01", DATE, (short) 1);

        assertThat(result).isEmpty();
        // @Retry(max-attempts=3) → 3회 호출 (circuit 은 min-calls=5 미만이라 단일 fetch 중 CLOSED)
        WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo("/api/mes/shift")));
    }

    @Test
    @DisplayName("시나리오3 timeout — 응답 지연 > timeout → fallback empty")
    void scenario3_timeout_fallback() {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withFixedDelay(3000)   // timeout-seconds=1 → read timeout
            .withBody("{}")));

        Optional<MesShiftResponse> result = client.fetchShift("LP-01", DATE, (short) 1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("시나리오4 부분 응답 (machineId 누락) — isComplete=false → empty")
    void scenario4_partial_response_skipped() {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/mes/shift")).willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {"shiftDate":"2026-05-29","shiftNo":1,"plannedQty":144,"actualQty":130}""")));

        Optional<MesShiftResponse> result = client.fetchShift("LP-01", DATE, (short) 1);

        assertThat(result).isEmpty();
    }
}
