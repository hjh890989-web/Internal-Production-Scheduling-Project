package com.scheduling.vc.mes;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 23 ST-MES-1 — 외부 MES REST polling 클라이언트 (EP-MES-ADAPTER-1).
 *
 * <p>{@code scheduling.mes.adapter=http} 일 때만 bean 활성 (default jpa 모드는 미존재).
 * {@code GET ${base-url}/api/mes/shift?machine=&date=&shift_no=} + Bearer 인증.
 *
 * <p>Resilience4j {@code @Retry(name=mes)} + {@code @CircuitBreaker(name=mes)} — 3회 retry,
 * 5회 연속 실패 시 30초 OPEN (application.yml). 실패·timeout·circuit OPEN 시 fallback →
 * {@link Optional#empty()} (호출부 {@link MesPollingService} 가 skip, degraded mode 가 임계 감지).
 *
 * <p>실 vendor spec 미확보 — mock contract 으로 baseline. Phase 5+ 실 spec 적용 시
 * {@link MesShiftResponse} 매핑만 교체.
 */
@Component
@ConditionalOnProperty(name = "scheduling.mes.adapter", havingValue = "http")
public class HttpMesShiftClient implements MesShiftClient {

    private static final Logger log = LoggerFactory.getLogger(HttpMesShiftClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String bearerToken;

    public HttpMesShiftClient(
        @Value("${scheduling.mes.http.base-url:}") String baseUrl,
        @Value("${scheduling.mes.http.bearer-token:}") String bearerToken,
        @Value("${scheduling.mes.http.timeout-seconds:10}") int timeoutSeconds
    ) {
        this.baseUrl = baseUrl;
        this.bearerToken = bearerToken;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    @Retry(name = "mes")
    @CircuitBreaker(name = "mes", fallbackMethod = "fetchFallback")
    public Optional<MesShiftResponse> fetchShift(String machineId, LocalDate shiftDate, short shiftNo) {
        MesShiftResponse body = restClient.get()
            .uri(baseUrl + "/api/mes/shift?machine={m}&date={d}&shift_no={s}",
                machineId, shiftDate.toString(), shiftNo)
            .headers(h -> {
                if (bearerToken != null && !bearerToken.isBlank()) {
                    h.setBearerAuth(bearerToken);
                }
            })
            .retrieve()
            .body(MesShiftResponse.class);

        if (body == null || !body.isComplete()) {
            log.warn("MES 부분/빈 응답 — machine={} date={} shift={} → skip", machineId, shiftDate, shiftNo);
            return Optional.empty();
        }
        log.debug("MES fetch 성공 — machine={} date={} shift={} actual={}",
            machineId, shiftDate, shiftNo, body.actualQty());
        return Optional.of(body);
    }

    /** Resilience4j fallback — retry 소진 / circuit OPEN / timeout. 다음 polling cycle 재시도. */
    @SuppressWarnings("unused")
    private Optional<MesShiftResponse> fetchFallback(String machineId, LocalDate shiftDate,
                                                     short shiftNo, Throwable t) {
        log.warn("MES fetch fallback (Resilience4j) — machine={} date={} shift={} cause={}",
            machineId, shiftDate, shiftNo, t.getClass().getSimpleName());
        return Optional.empty();
    }
}
