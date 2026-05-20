package com.scheduling.order.watcher;

/**
 * picked_file 상태 머신 — TK-01-3-2.
 *
 * <pre>
 *   QUEUED → PROCESSING → INGESTED
 *                       → FAILED
 *
 *   SKIPPED_DUPLICATE  (24h 윈도우 내 동일 해시 발견 시)
 * </pre>
 */
public enum PickedFileStatus {
    QUEUED,
    PROCESSING,
    INGESTED,
    SKIPPED_DUPLICATE,
    FAILED
}
