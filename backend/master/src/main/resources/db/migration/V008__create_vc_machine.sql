-- =============================================================================
-- V008__create_vc_machine.sql — TK-05-1-1 (SAD §6.2.6, BR-V05)
-- =============================================================================
-- 가류기 마스터 — BR-V05: 저압 (LP) 4대 × 8슬롯 + IC 1대 × 6슬롯.
-- 회전 정의 — 주간 8 + 야간 10 = 18 회전/대/일 (BR-V04 / REF-11).
-- schema: master.
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.vc_machine (
    machine_id        VARCHAR(10) PRIMARY KEY,
    machine_type      VARCHAR(2)  NOT NULL CHECK (machine_type IN ('LP','IC')),
    total_slots       SMALLINT    NOT NULL CHECK (total_slots > 0),
    day_rotations     SMALLINT    NOT NULL DEFAULT 8  CHECK (day_rotations >= 0),
    night_rotations   SMALLINT    NOT NULL DEFAULT 10 CHECK (night_rotations >= 0),
    active            BOOLEAN     NOT NULL DEFAULT true,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        VARCHAR(40) NOT NULL DEFAULT 'system:seed',

    CONSTRAINT chk_lp_slots CHECK (machine_type <> 'LP' OR total_slots = 8),
    CONSTRAINT chk_ic_slots CHECK (machine_type <> 'IC' OR total_slots = 6)
);

CREATE INDEX IF NOT EXISTS idx_vc_machine_active_type
    ON master.vc_machine (active, machine_type);

COMMENT ON TABLE  master.vc_machine                 IS 'TK-05-1-1 BR-V05 가류기 마스터 — LP 4대(8슬롯) + IC 1대(6슬롯)';
COMMENT ON COLUMN master.vc_machine.day_rotations   IS '주간 회전수 (BR-V04 기본 8)';
COMMENT ON COLUMN master.vc_machine.night_rotations IS '야간 회전수 (BR-V04 기본 10) — 합계 18 회전/일';
