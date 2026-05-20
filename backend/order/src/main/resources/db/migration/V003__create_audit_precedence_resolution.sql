-- =============================================================================
-- V003__create_audit_precedence_resolution.sql — TK-02-2-2
-- =============================================================================
-- 중복 그룹 우선순위 해소 audit 테이블. BR-X02 (모든 mutation audit) 강제.
-- audit schema — REQ-NF-SEC-004 (≥3년 보존 + INSERT-only).
-- Sprint 1+ EP-11 audit 모듈 활성 후 본 테이블에 row INSERT.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.precedence_resolution (
    resolution_id    UUID         PRIMARY KEY,
    hose_id          VARCHAR(40)  NOT NULL,
    delivery_date    DATE         NOT NULL,
    decision         VARCHAR(32)  NOT NULL CHECK (decision IN ('KEPT_EXISTING','REPLACED_EXISTING','NEW_WINS')),
    winner_type      VARCHAR(20)  NOT NULL CHECK (winner_type IN ('FORECAST','KD','WEEKLY','CONFIRMED')),
    winner_order_id  UUID         NOT NULL,
    loser_types      VARCHAR(255) NOT NULL DEFAULT '[]',
    existing_type    VARCHAR(20),
    actor            VARCHAR(40)  NOT NULL,
    resolved_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- BR-X02 — INSERT only 강제 (UPDATE·DELETE 거부)
-- 본 정책의 trigger 는 EP-11 ST-11-2 (Sprint 2) 에서 별도 마이그레이션으로 부착.
-- Sprint 1 baseline 은 role 권한으로 강제 (postgres/init/02_grant_permissions.sql).

CREATE INDEX IF NOT EXISTS idx_audit_precedence_hose_date
    ON audit.precedence_resolution (hose_id, delivery_date);

CREATE INDEX IF NOT EXISTS idx_audit_precedence_resolved_at
    ON audit.precedence_resolution (resolved_at);

COMMENT ON TABLE  audit.precedence_resolution         IS 'TK-02-2-2 우선순위 해소 audit (BR-X02 + REQ-FUNC-OC-006)';
COMMENT ON COLUMN audit.precedence_resolution.decision IS 'KEPT_EXISTING / REPLACED_EXISTING / NEW_WINS';
COMMENT ON COLUMN audit.precedence_resolution.actor    IS '해소 트리거 actor (Sprint 1+ JWT subject)';
