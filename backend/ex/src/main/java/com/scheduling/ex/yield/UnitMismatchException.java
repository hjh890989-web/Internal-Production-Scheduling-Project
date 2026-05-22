package com.scheduling.ex.yield;

/**
 * 단위 변환 오류 — TK-08-2-3 (BR-E05 단위 가드).
 *
 * <p>yield 수식 입력 시 단위 명시 — speed 는 m/min, length 는 mm.
 * speed 값이 비현실적이면 마스터 입력 오류 (예: m/min 대신 mm/min) 가능성 — fail-fast.
 */
public class UnitMismatchException extends RuntimeException {
    public UnitMismatchException(String message) {
        super(message);
    }
}
