package com.scheduling.order.mapping;

import java.util.List;

/**
 * 매핑 실패 row — TK-01-2-1.
 *
 * <p>{@link com.scheduling.order.api.SchemaMappingService} 가 단일 row 매핑 시도 중 발생한
 * 실패를 본 record 로 보존. TK-01-2-2 보정 UI 가 사용자에게 제시.
 *
 * @param sheetName        실패 발생 sheet
 * @param rowIndex         0-based row index
 * @param originalCells    원본 row 셀 값 (보정 UI 표시용)
 * @param failedField      실패 필드 (hose_id·delivery_date·qty·customer·HEADER)
 * @param reason           NFR-USA-002 사용자 노출 사유
 */
public record MappingFailure(
    String sheetName,
    int rowIndex,
    List<String> originalCells,
    String failedField,
    String reason
) {
    public MappingFailure {
        originalCells = originalCells == null ? List.of() : List.copyOf(originalCells);
    }

    /** 사용자 노출 메시지 (NFR-USA-002 설명적 피드백). */
    public String userMessage() {
        return "%s 시트 %d행: %s — %s".formatted(sheetName, rowIndex + 1, failedField, reason);
    }
}
