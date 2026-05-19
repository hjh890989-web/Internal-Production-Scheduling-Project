package com.scheduling.order.import_;

import com.scheduling.order.api.ImportStatusResponse;
import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ImportTrackingService 단위 테스트 — TK-01-1-3.
 *
 * <p>Redis 동작은 StringRedisTemplate 모킹 — 통합 테스트(@DataRedisTest)는 Sprint 1+.
 */
class ImportTrackingServiceTest {

    private StringRedisTemplate redis;
    private HashOperations<String, Object, Object> hashOps;
    private Clock clock;
    private ImportTrackingService service;
    private final Map<String, Map<String, String>> store = new HashMap<>();

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        hashOps = (HashOperations) mock(HashOperations.class);
        clock = Clock.fixed(Instant.parse("2026-05-19T05:00:00Z"), ZoneId.of("Asia/Seoul"));
        when(redis.opsForHash()).thenReturn(hashOps);

        // hash put / putAll / entries 단순 in-memory 모의
        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            String field = inv.getArgument(1).toString();
            String value = inv.getArgument(2).toString();
            store.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(field, value);
            return null;
        }).when(hashOps).put(anyString(), any(), any());

        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            Map<String, String> data = inv.getArgument(1);
            store.computeIfAbsent(key, k -> new LinkedHashMap<>()).putAll(data);
            return null;
        }).when(hashOps).putAll(anyString(), any());

        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            String field = inv.getArgument(1).toString();
            Map<String, String> data = store.get(key);
            return data == null ? null : data.get(field);
        }).when(hashOps).get(anyString(), any());

        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            Map<String, String> data = store.get(key);
            if (data == null) return Map.of();
            Map<Object, Object> result = new LinkedHashMap<>();
            data.forEach(result::put);
            return result;
        }).when(hashOps).entries(anyString());

        service = new ImportTrackingService(redis, clock);
    }

    @Test
    @DisplayName("markStarted — 추적 ID 등록 + filenames 보존")
    void markStarted_basic() {
        UUID id = UUID.randomUUID();
        service.markStarted(id, List.of("a.xlsx", "b.xlsx"));

        ImportStatusResponse status = service.get(id);
        assertThat(status.trackingId()).isEqualTo(id);
        assertThat(status.status()).isEqualTo(ImportStatus.QUEUED);
        assertThat(status.filenames()).containsExactly("a.xlsx", "b.xlsx");
        assertThat(status.error()).isNull();

        verify(redis).expire(eq("scheduling:import:" + id), any());
    }

    @Test
    @DisplayName("update — 상태 전이 + updatedAt 갱신")
    void update_status() {
        UUID id = UUID.randomUUID();
        service.markStarted(id, List.of("x.xlsx"));
        service.update(id, ImportStatus.PARSING, null);

        ImportStatusResponse status = service.get(id);
        assertThat(status.status()).isEqualTo(ImportStatus.PARSING);
    }

    @Test
    @DisplayName("update — FAILED + 에러 메시지")
    void update_failed_with_error() {
        UUID id = UUID.randomUUID();
        service.markStarted(id, List.of("x.xlsx"));
        service.update(id, ImportStatus.FAILED, "POI parse error");

        ImportStatusResponse status = service.get(id);
        assertThat(status.status()).isEqualTo(ImportStatus.FAILED);
        assertThat(status.error()).isEqualTo("POI parse error");
    }

    @Test
    @DisplayName("recordClassification — 파일별 분류 결과 누적")
    void record_classification_multiple() {
        UUID id = UUID.randomUUID();
        service.markStarted(id, List.of("a.xlsx", "b.xlsx"));

        service.recordClassification(id, "a.xlsx",
            new ClassificationResult(SourceType.MONTHLY_FORECAST, 0.95, Map.of()));
        service.recordClassification(id, "b.xlsx",
            new ClassificationResult(SourceType.WEEKLY_PLAN, 0.80, Map.of()));

        ImportStatusResponse status = service.get(id);
        assertThat(status.classifications())
            .containsEntry("a.xlsx", "MONTHLY_FORECAST:0.95")
            .containsEntry("b.xlsx", "WEEKLY_PLAN:0.80");
    }

    @Test
    @DisplayName("get — 존재하지 않는 ID → NoSuchElementException")
    void get_not_found() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.get(id))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining(id.toString());
    }
}
