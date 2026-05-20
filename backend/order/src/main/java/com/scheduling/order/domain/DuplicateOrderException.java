package com.scheduling.order.domain;

import java.time.LocalDate;

/**
 * 중복 수주 commit 실패 — TK-02-1-1 (REQ-FUNC-OC-005).
 *
 * <p>발생 경로:
 * <ol>
 *   <li>DB: PostgreSQL UNIQUE 제약 위반 → DataIntegrityViolationException → 본 예외 변환</li>
 *   <li>ORM 사전: {@link DuplicateDetectionService#detect} 가 사전 차단 (DB hit 회피)</li>
 * </ol>
 *
 * <p>{@code OrderExceptionHandler} 가 HTTP 409 Conflict + 한국어 ProblemDetail 변환.
 */
public class DuplicateOrderException extends RuntimeException {

    private final String hoseId;
    private final LocalDate deliveryDate;
    private final int masterVersion;

    public DuplicateOrderException(String hoseId, LocalDate deliveryDate, int masterVersion) {
        super("중복 수주: 품번=%s, 납기=%s, 버전=%d (REQ-FUNC-OC-005)"
            .formatted(hoseId, deliveryDate, masterVersion));
        this.hoseId = hoseId;
        this.deliveryDate = deliveryDate;
        this.masterVersion = masterVersion;
    }

    public DuplicateOrderException(String hoseId, LocalDate deliveryDate, int masterVersion, Throwable cause) {
        super("중복 수주: 품번=%s, 납기=%s, 버전=%d (REQ-FUNC-OC-005)"
            .formatted(hoseId, deliveryDate, masterVersion), cause);
        this.hoseId = hoseId;
        this.deliveryDate = deliveryDate;
        this.masterVersion = masterVersion;
    }

    public String getHoseId() {
        return hoseId;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public int getMasterVersion() {
        return masterVersion;
    }
}
