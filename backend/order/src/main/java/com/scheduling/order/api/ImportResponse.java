package com.scheduling.order.api;

import com.scheduling.order.import_.ImportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Import 즉시 응답 (HTTP 202) — TK-01-1-3.
 *
 * @param trackingId  추적 ID (UUID)
 * @param status      현재 상태 (QUEUED)
 * @param statusUrl   상태 조회 URL
 * @param filenames   접수된 파일명
 * @param acceptedAt  접수 시각 (KST ISO-8601)
 */
public record ImportResponse(
    UUID trackingId,
    ImportStatus status,
    String statusUrl,
    List<String> filenames,
    Instant acceptedAt
) {}
