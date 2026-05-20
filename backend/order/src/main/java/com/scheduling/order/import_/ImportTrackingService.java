package com.scheduling.order.import_;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.order.api.ImportStatusResponse;
import com.scheduling.order.mapping.MappingResult;
import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.ParsedWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Import 추적 상태 저장소 — TK-01-1-3.
 *
 * <p>Redis Hash 기반:
 * <pre>
 *   key:    "scheduling:import:{trackingId}"
 *   fields: status, startedAt, updatedAt, filenames(JSON), classifications(JSON), error
 *   TTL:    24h
 * </pre>
 *
 * <p>JSON 직렬화는 단순 string concatenation — Sprint 1+ Jackson 통합 검토.
 */
@Service
public class ImportTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ImportTrackingService.class);
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "scheduling:import:";

    private static final String PARSED_KEY_PREFIX = "scheduling:import:parsed:";
    private static final String MAPPING_KEY_PREFIX = "scheduling:import:mapping:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ImportTrackingService(StringRedisTemplate redis, ObjectMapper objectMapper, Clock clock) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void markStarted(UUID trackingId, List<String> filenames) {
        String key = key(trackingId);
        Instant now = Instant.now(clock);

        Map<String, String> data = new HashMap<>();
        data.put("status", ImportStatus.QUEUED.name());
        data.put("startedAt", now.toString());
        data.put("updatedAt", now.toString());
        data.put("filenames", String.join("||", filenames));
        data.put("classifications", "");
        data.put("error", "");

        redis.opsForHash().putAll(key, data);
        redis.expire(key, TTL);
        log.info("Import tracking started: {} ({} files)", trackingId, filenames.size());
    }

    public void update(UUID trackingId, ImportStatus status, String error) {
        String key = key(trackingId);
        Map<String, String> updates = new HashMap<>();
        updates.put("status", status.name());
        updates.put("updatedAt", Instant.now(clock).toString());
        if (error != null) {
            updates.put("error", error);
        }
        redis.opsForHash().putAll(key, updates);
    }

    public void recordClassification(UUID trackingId, String filename, ClassificationResult result) {
        String key = key(trackingId);
        String existing = (String) redis.opsForHash().get(key, "classifications");
        String entry = filename + "=" + result.sourceType().name() + ":"
                     + String.format("%.2f", result.confidence());
        String updated = (existing == null || existing.isEmpty())
            ? entry
            : existing + "||" + entry;
        redis.opsForHash().put(key, "classifications", updated);
        redis.opsForHash().put(key, "updatedAt", Instant.now(clock).toString());
    }

    public ImportStatusResponse get(UUID trackingId) {
        String key = key(trackingId);
        Map<Object, Object> raw = redis.opsForHash().entries(key);
        if (raw.isEmpty()) {
            throw new NoSuchElementException("Tracking ID not found: " + trackingId);
        }

        ImportStatus status = ImportStatus.valueOf(asString(raw.get("status")));
        Instant startedAt = Instant.parse(asString(raw.get("startedAt")));
        Instant updatedAt = Instant.parse(asString(raw.get("updatedAt")));
        List<String> filenames = parseList(asString(raw.get("filenames")));
        Map<String, String> classifications = parseClassifications(asString(raw.get("classifications")));
        String error = asString(raw.get("error"));
        if (error.isEmpty()) error = null;

        return new ImportStatusResponse(
            trackingId, status, startedAt, updatedAt, filenames, classifications, error);
    }

    private String asString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private List<String> parseList(String packed) {
        return packed.isEmpty() ? List.of() : List.of(packed.split("\\|\\|"));
    }

    private Map<String, String> parseClassifications(String packed) {
        if (packed.isEmpty()) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (String entry : packed.split("\\|\\|")) {
            int eq = entry.indexOf('=');
            if (eq > 0) {
                result.put(entry.substring(0, eq), entry.substring(eq + 1));
            }
        }
        return result;
    }

    private String key(UUID trackingId) {
        return KEY_PREFIX + trackingId;
    }

    // -----------------------------------------------------------------------
    // TK-01-2-3 — ParsedWorkbook 캐시 (라운드트립 재매핑)
    // -----------------------------------------------------------------------

    /**
     * Parse 결과 캐시 — 사용자 룰 수정 후 retry 시 재업로드 회피 (REQ-FUNC-OC-004).
     * TTL 24h.
     */
    public void cacheParsedWorkbooks(UUID trackingId, List<ParsedWorkbook> workbooks) {
        String key = PARSED_KEY_PREFIX + trackingId;
        try {
            String payload = objectMapper.writeValueAsString(workbooks);
            redis.opsForValue().set(key, payload, TTL);
            log.info("Cached {} parsed workbooks for trackingId={} ({}KB)",
                workbooks.size(), trackingId, payload.length() / 1024);
        } catch (Exception e) {
            throw new IllegalStateException("ParsedWorkbook 직렬화 실패: " + trackingId, e);
        }
    }

    /**
     * 캐시된 ParsedWorkbook 로드. TTL 만료 시 {@link NoSuchElementException}.
     */
    public List<ParsedWorkbook> loadParsedWorkbooks(UUID trackingId) {
        String key = PARSED_KEY_PREFIX + trackingId;
        String payload = redis.opsForValue().get(key);
        if (payload == null) {
            throw new NoSuchElementException("ParsedWorkbook 캐시 만료 또는 미존재: " + trackingId);
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<ParsedWorkbook>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("ParsedWorkbook 역직렬화 실패: " + trackingId, e);
        }
    }

    /**
     * 매핑 결과 캐시 — UI 가 즉시 조회 (실패 row 보정 모달 입력).
     */
    public void cacheMappingResult(UUID trackingId, MappingResult result) {
        String key = MAPPING_KEY_PREFIX + trackingId;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            throw new IllegalStateException("MappingResult 직렬화 실패: " + trackingId, e);
        }
    }

    public MappingResult loadMappingResult(UUID trackingId) {
        String key = MAPPING_KEY_PREFIX + trackingId;
        String payload = redis.opsForValue().get(key);
        if (payload == null) {
            throw new NoSuchElementException("MappingResult 캐시 없음: " + trackingId);
        }
        try {
            return objectMapper.readValue(payload, MappingResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("MappingResult 역직렬화 실패: " + trackingId, e);
        }
    }
}
