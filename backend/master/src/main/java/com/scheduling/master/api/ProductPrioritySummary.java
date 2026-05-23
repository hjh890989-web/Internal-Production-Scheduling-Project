package com.scheduling.master.api;

import java.time.LocalDate;

/**
 * BR-V12 PRODUCT_PRIORITY 요약 — Sprint 7 (cross-module).
 *
 * <p>vc 모듈 {@code CapacityOverflowQueueService} 가 본 record 만 받음.
 */
public record ProductPrioritySummary(
    String hoseId,
    short priorityRank,         // 1=최우선
    String rationale,
    LocalDate effectiveFrom,
    LocalDate effectiveTo       // null = 무기한
) {}
