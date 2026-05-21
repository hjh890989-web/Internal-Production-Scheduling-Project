-- =============================================================================
-- V010__create_vc_schedule.sql — TK-05-1-1 (SAD §6.2.7, REQ-FUNC-VC-005)
-- =============================================================================
-- 회전 단위 성형 스케줄 — (date, machineId, rotationNo 1~18, slotPosition) PK.
-- ADR-005 — 시간이 아닌 회전이 단위.
--
-- schema: app (operational data). master.product / master.vc_machine FK 는
-- master.product 미존재로 인해 application-level 검증 (Sprint 2 baseline).
-- Phase 2 master.product 도입 후 ALTER TABLE 로 FK 추가.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.vc_schedule (
    vc_schedule_id      UUID         PRIMARY KEY,
    hose_id             VARCHAR(40)  NOT NULL,
    machine_id          VARCHAR(10)  NOT NULL REFERENCES master.vc_machine(machine_id),
    slot_position       SMALLINT     NOT NULL CHECK (slot_position BETWEEN 1 AND 8),
    production_date     DATE         NOT NULL,
    rotation_no         SMALLINT     NOT NULL CHECK (rotation_no BETWEEN 1 AND 18),   -- BR-V04
    angle_id            VARCHAR(40)  NOT NULL,
    planned_qty         INTEGER      NOT NULL CHECK (planned_qty >= 0),
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('CANDIDATE','CONFIRMED','DONE')),
    linked_order_ids    TEXT         NOT NULL DEFAULT '',   -- comma-separated UUIDs (Sprint 2 baseline; PG UUID[] 는 Phase 2)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 한 (machine, slot, date, rotation) 칸에 1 스케줄만
    CONSTRAINT uq_vc_schedule_slot_rotation
        UNIQUE (machine_id, slot_position, production_date, rotation_no)
);

CREATE INDEX IF NOT EXISTS idx_vc_schedule_date_status
    ON app.vc_schedule (production_date, status);

CREATE INDEX IF NOT EXISTS idx_vc_schedule_hose
    ON app.vc_schedule (hose_id);

CREATE INDEX IF NOT EXISTS idx_vc_schedule_machine_date
    ON app.vc_schedule (machine_id, production_date);

COMMENT ON TABLE  app.vc_schedule                  IS 'TK-05-1-1 회전 단위 성형 스케줄 (REQ-FUNC-VC-005)';
COMMENT ON COLUMN app.vc_schedule.rotation_no      IS 'BR-V04 1-8 주간, 9-18 야간';
COMMENT ON COLUMN app.vc_schedule.linked_order_ids IS 'comma-separated UUIDs — Phase 2 UUID[] 로 ALTER';
