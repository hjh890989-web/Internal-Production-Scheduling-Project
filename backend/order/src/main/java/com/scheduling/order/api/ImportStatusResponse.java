package com.scheduling.order.api;

import com.scheduling.order.import_.ImportStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Import 상태 조회 응답 (GET /api/v1/orders/import/{trackingId}) — TK-01-1-3.
 *
 * @param trackingId       추적 ID
 * @param status           현재 상태
 * @param startedAt        접수 시각
 * @param updatedAt        마지막 갱신 시각
 * @param filenames        접수된 파일명
 * @param classifications  파일별 분류 결과 (PARSED 단계 이후)
 * @param error            FAILED 시 에러 메시지 (그 외 null)
 */
public record ImportStatusResponse(
    UUID trackingId,
    ImportStatus status,
    Instant startedAt,
    Instant updatedAt,
    List<String> filenames,
    Map<String, String> classifications,
    String error
) {}
