package com.scheduling.order.import_;

/**
 * Import 진행 상태 — TK-01-1-3.
 *
 * <p>전이: QUEUED → PARSING → PARSED → MAPPED (ST-01-2) → COMMITTED (ST-01-2)
 * 실패 시 어디서든 FAILED.
 */
public enum ImportStatus {
    QUEUED,       // 추적 ID 발급, 비동기 큐 적재
    PARSING,      // ExcelParserService 실행 중
    PARSED,       // 파싱 + 분류 완료 (다음 단계 = ST-01-2 매핑)
    MAPPED,       // ST-01-2 매핑 완료
    COMMITTED,    // ST-01-2 DB commit 완료
    FAILED        // 어느 단계에서든 실패
}
