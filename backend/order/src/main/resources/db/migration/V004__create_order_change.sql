-- =============================================================================
-- V004__create_order_change.sql — TK-03-1-3
-- =============================================================================
-- SAD §6.2.5 ORDER_CHANGE — 마스터 버전 간 row-level diff 영속화.
-- REQ-FUNC-OC-007 (diff 정확) + REQ-FUNC-OC-014 (시점 복원).
-- severity column = ST-03-2 (Critical 태깅) 후속 — 본 마이그레이션에서는 NULL 허용.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.order_change (
    change_id         UUID         PRIMARY KEY,
    tracking_id       UUID         NOT NULL,
    diff_type         VARCHAR(20)  NOT NULL CHECK (diff_type IN ('NEW','MODIFIED','DELETED','UNCHANGED')),
    hose_id           VARCHAR(40)  NOT NULL,
    delivery_date     DATE         NOT NULL,
    new_order_id      UUID,
    old_order_id      UUID,
    field_diffs       JSONB,
    previous_version  INTEGER      NOT NULL,
    new_version       INTEGER      NOT NULL,
    severity          VARCHAR(10),
    changed_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_change_tracking
    ON app.order_change (tracking_id);
CREATE INDEX IF NOT EXISTS idx_order_change_hose_date
    ON app.order_change (hose_id, delivery_date);
CREATE INDEX IF NOT EXISTS idx_order_change_severity
    ON app.order_change (severity)
    WHERE severity IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_change_version
    ON app.order_change (previous_version, new_version);

COMMENT ON TABLE  app.order_change                  IS 'TK-03-1-3 row-level diff 영속 (REQ-FUNC-OC-007·014)';
COMMENT ON COLUMN app.order_change.field_diffs      IS '[{"fieldName":..., "before":..., "after":...}, ...] — JSONB';
COMMENT ON COLUMN app.order_change.severity         IS 'ST-03-2 (TK-03-2-*) 에서 채움 — CRITICAL / WARNING / INFO';
