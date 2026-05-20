package com.scheduling.order.api;

/**
 * 중복 수주 응답 DTO (HTTP 409 Conflict) — TK-02-1-1.
 *
 * @param errorCode      "DUPLICATE_ORDER"
 * @param message        한국어 사용자 메시지 (NFR-USA-002·003)
 * @param hoseId         중복 위반 hose_id
 * @param deliveryDate   ISO-8601 (yyyy-MM-dd)
 * @param masterVersion  중복 위반 master_version
 */
public record DuplicateOrderResponse(
    String errorCode,
    String message,
    String hoseId,
    String deliveryDate,
    int masterVersion
) {}
