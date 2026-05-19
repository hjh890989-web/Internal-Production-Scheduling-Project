package com.scheduling.order.parser;

/**
 * 엑셀 파싱 실패 — TK-01-1-1.
 *
 * <p>code 값:
 * <ul>
 *   <li>{@code FILE_TOO_LARGE} — 20MB 초과 (HTTP 413 매핑)</li>
 *   <li>{@code PARSE_FAILED} — POI 가 .xlsx 파싱 실패 (손상·비표준)</li>
 *   <li>{@code UNRECOGNIZED_FORMAT} — TK-01-1-2 분류기가 4종 어디에도 해당 안 됨</li>
 * </ul>
 *
 * <p>Sprint 1+ {@code @RestControllerAdvice} 가 본 예외를 RFC 7807 ProblemDetail 로 변환.
 */
public class ExcelParseException extends RuntimeException {

    private final String code;

    public ExcelParseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ExcelParseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
