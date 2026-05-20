-- =============================================================================
-- V006__create_picked_file.sql — TK-01-3-2 (REQ-FUNC-OC-015, BR-X02)
-- =============================================================================
-- folder watcher 가 ingest 한 파일 audit 기록 + 중복 처리.
--
-- schema: app (operational queue — 상태 진행 QUEUED→PROCESSING→INGESTED 필요).
-- Phase 2 audit 강화 — picked_file_event 별도 테이블 (event sourcing) 도입 검토.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.picked_file (
    picked_file_id  UUID         PRIMARY KEY,
    file_path       TEXT         NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_hash       VARCHAR(64)  NOT NULL,                                 -- SHA-256 hex (JPA @Column length=64)
    size_bytes      BIGINT       NOT NULL,
    discovered_at   TIMESTAMPTZ  NOT NULL,
    discovered_via  VARCHAR(20)  NOT NULL CHECK (discovered_via IN ('WATCH_SERVICE','SCHEDULED_POLL')),
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('QUEUED','PROCESSING','INGESTED','SKIPPED_DUPLICATE','FAILED')),
    tracking_id     UUID,
    error_message   TEXT,
    processed_at    TIMESTAMPTZ,
    actor           VARCHAR(40)  NOT NULL DEFAULT 'system:watcher'
);

CREATE INDEX IF NOT EXISTS idx_picked_file_hash
    ON app.picked_file (file_hash);

CREATE INDEX IF NOT EXISTS idx_picked_file_status_time
    ON app.picked_file (status, discovered_at DESC);

CREATE INDEX IF NOT EXISTS idx_picked_file_duplicate_window
    ON app.picked_file (file_hash, discovered_at)
    WHERE status IN ('INGESTED', 'PROCESSING');

COMMENT ON TABLE  app.picked_file               IS 'TK-01-3-2 watcher ingest audit + 중복 처리 큐';
COMMENT ON COLUMN app.picked_file.file_hash     IS 'SHA-256 hex — 중복 검출 키';
COMMENT ON COLUMN app.picked_file.status        IS '상태머신: QUEUED→PROCESSING→INGESTED|FAILED, 또는 SKIPPED_DUPLICATE';
COMMENT ON COLUMN app.picked_file.actor         IS 'system:watcher (사용자 직접 업로드와 구분)';
