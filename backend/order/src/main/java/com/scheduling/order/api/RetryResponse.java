package com.scheduling.order.api;

import com.scheduling.order.import_.ImportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 재매핑 응답 (HTTP 202) — TK-01-2-3.
 *
 * @param trackingId   기존 추적 ID (재시도)
 * @param status       QUEUED (즉시 응답, 비동기 처리 시작)
 * @param statusUrl    진행 상태 조회 URL
 * @param triggeredAt  재시도 트리거 시각 (KST)
 */
public record RetryResponse(
    UUID trackingId,
    ImportStatus status,
    String statusUrl,
    Instant triggeredAt
) {}
