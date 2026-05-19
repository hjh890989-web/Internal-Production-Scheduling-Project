package com.scheduling.order.import_;

import com.scheduling.order.api.ImportStatusResponse;
import com.scheduling.order.parser.ClassificationResult;
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

    private final StringRedisTemplate redis;
    private final Clock clock;

    public ImportTrackingService(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
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
}
