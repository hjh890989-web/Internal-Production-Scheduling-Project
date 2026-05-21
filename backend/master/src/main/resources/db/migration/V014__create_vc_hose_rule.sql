-- =============================================================================
-- V014__create_vc_hose_rule.sql — TK-21-2-1 (EP-21 ST-21-2)
-- =============================================================================
-- 품번 단위 운영 룰 — 호기 핀·동시 슬롯 상한·좌/우 락·LP 전용.
-- BR-V14 hard 제약. PostgreSQL LISTEN/NOTIFY 트리거 — TK-21-2-3 캐시 무효화.
--
-- DDL 정합:
--   machine_pin VARCHAR(10) — JPA varchar (Sprint 1 CHAR/VARCHAR 충돌 패턴)
--   max_concurrent_slots 1~99 (CHECK), DEFAULT 99 = 사실상 무제한
--   side_lock VARCHAR(5) IN ('LEFT','RIGHT') — NULL = 양쪽
--   lp_only BOOLEAN DEFAULT FALSE
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.vc_hose_rule (
    hose_id              VARCHAR(40) PRIMARY KEY,
    machine_pin          VARCHAR(10),
    max_concurrent_slots INTEGER     NOT NULL DEFAULT 99
        CHECK (max_concurrent_slots BETWEEN 1 AND 99),
    side_lock            VARCHAR(5)
        CHECK (side_lock IS NULL OR side_lock IN ('LEFT','RIGHT')),
    lp_only              BOOLEAN     NOT NULL DEFAULT FALSE,
    notes                TEXT,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           VARCHAR(40) NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_vc_hose_rule_machine_pin
    ON master.vc_hose_rule (machine_pin) WHERE machine_pin IS NOT NULL;

COMMENT ON TABLE  master.vc_hose_rule                     IS
    'EP-21 ST-21-2 품번 단위 운영 룰 (BR-V14, REQ-FUNC-VC-024)';
COMMENT ON COLUMN master.vc_hose_rule.machine_pin         IS
    '고정 가류기 (예: 28422-08HA0 → LP-01). NULL = 자유 배치';
COMMENT ON COLUMN master.vc_hose_rule.max_concurrent_slots IS
    '동시 다중 슬롯 상한 (Σ ≤ value, 99 = 사실상 무제한)';
COMMENT ON COLUMN master.vc_hose_rule.side_lock           IS
    '좌/우 강제 (28422-2M800 → RIGHT). NULL = K/L setting 만 적용';
COMMENT ON COLUMN master.vc_hose_rule.lp_only             IS
    'IC 사용 금지 (BR-V08 LP-first 강화)';

-- TK-21-2-3 LISTEN/NOTIFY 트리거
CREATE OR REPLACE FUNCTION master.notify_vc_hose_rule_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('vc_hose_rule_changed',
        COALESCE(NEW.hose_id, OLD.hose_id));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_hose_rule_notify ON master.vc_hose_rule;
CREATE TRIGGER trg_vc_hose_rule_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.vc_hose_rule
    FOR EACH ROW EXECUTE FUNCTION master.notify_vc_hose_rule_change();
