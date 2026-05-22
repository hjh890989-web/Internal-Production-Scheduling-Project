package com.scheduling.master.api;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Shift 요약 DTO — TK-08-1-1 (cross-module).
 *
 * <p>{@code effective_min} = FLOOR(nominalMin × efficiency) — DB GENERATED 값.
 * ex 모듈의 yield 수식 입력 (BR-E05).
 */
public record ShiftSummary(
    String shiftCode,
    String name,
    LocalTime startTime,
    LocalTime endTime,
    int nominalMin,
    BigDecimal efficiency,
    int effectiveMin,
    short sortOrder
) {}
