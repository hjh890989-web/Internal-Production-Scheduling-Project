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
 * Sprint 23 ST-MES-1 — 외부 MES REST polling 클라이언트 (Adapter 패턴 — read/fetch side).
 *
 * <h2>Adapter 패턴 위치</h2>
 * <p>{@link MesShiftClient} 구현체 (fetch Port). {@code scheduling.mes.adapter=http} 일 때만
 * bean 활성. default {@code jpa} 모드에서는 이 bean 미존재 — {@link MesPollingService} 도
 * {@code adapter=http} 조건부 활성이므로 jpa 모드에서는 polling scheduler 자체 미동작.
 *
 * <p>{@code GET ${base-url}/api/mes/shift?machine=&date=&shift_no=} + Bearer 인증.
 *
 * <p>Resilience4j {@code @Retry(name=mes)} + {@code @CircuitBreaker(name=mes)} — 3회 retry,
 * 5회 연속 실패 시 30초 OPEN (application.yml). 실패·timeout·circuit OPEN 시 fallback →
 * {@link Optional#empty()} (호출부 {@link MesPollingService} 가 skip, degraded mode 가 임계 감지).
 *
 * <p>실 vendor spec 미확보 — mock contract 기준 ({@link MesShiftResponse} 필드 참조).
 * Sprint 26 S26-B: vendor 실 spec 수신 후 {@link MesShiftResponse} DTO 필드 재정의 예정.
 * Phase 5+ 실 spec 적용 시 {@link MesShiftResponse} 매핑만 교체 — 본 클래스 로직 불변.
 *
 * @see MesShiftClient
 * @see MesShiftResponse
 * @see MesPollingService
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

    // fallbackMethod 는 @Retry(최외곽 aspect)에 둔다 — @CircuitBreaker 에 두면 첫 실패에서
    // 즉시 fallback → @Retry 가 예외를 못 보고 재시도 0회. Retry(CB(method)) 순서에서
    // 3회 재시도 소진 후 fallback 발동 (circuit OPEN 시 CallNotPermittedException 은 retry 대상 외 → 즉시 empty).
    @Override
    @Retry(name = "mes", fallbackMethod = "fetchFallback")
    @CircuitBreaker(name = "mes")
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
