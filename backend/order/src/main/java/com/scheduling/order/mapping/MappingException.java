package com.scheduling.order.mapping;

/**
 * 단일 row 매핑 실패 — TK-01-2-1.
 *
 * <p>SchemaMappingService 내부에서 throw → catch 후 MappingFailure 변환.
 * 외부로 노출되지 않음 (runtime).
 */
public class MappingException extends RuntimeException {

    private final String field;
    private final String reason;

    public MappingException(String field, String reason) {
        super("Mapping failed for field=" + field + ", reason=" + reason);
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
