-- =============================================================================
-- V028__create_vc_schedule_swap_proposal.sql — TK-15-2-2 (EP-15 ST-15-2)
-- =============================================================================
-- 현장 작업자 (STK_USER) 가 회전 격자 cell 의 swap 제안 → Planner 1클릭 수용.
-- REQ-FUNC-VC-018: 총량 보존 (swap 후 hose 별 일별 총 yield 동일).
--
-- 상태 머신: PROPOSED → ACCEPTED / REJECTED.
-- 수용 시 atomic swap (두 row 의 rotation_no 만 교체) — 총량 보존 invariant.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.vc_schedule_swap_proposal (
    proposal_id      UUID         PRIMARY KEY,
    source_row_id    UUID         NOT NULL REFERENCES app.vc_schedule(vc_schedule_id),
    target_row_id    UUID         NOT NULL REFERENCES app.vc_schedule(vc_schedule_id),
    proposed_by      VARCHAR(40)  NOT NULL,           -- ROLE_STK_USER 사번
    proposed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reason           TEXT,
    status           VARCHAR(10)  NOT NULL CHECK (status IN ('PROPOSED','ACCEPTED','REJECTED')),
    resolved_by      VARCHAR(40),                      -- ROLE_PLANNER 사번
    resolved_at      TIMESTAMPTZ,
    resolution_note  TEXT,

    CONSTRAINT chk_distinct_rows CHECK (source_row_id <> target_row_id)
);

CREATE INDEX IF NOT EXISTS idx_swap_proposal_status
    ON app.vc_schedule_swap_proposal (status, proposed_at);
CREATE INDEX IF NOT EXISTS idx_swap_proposal_proposed_by
    ON app.vc_schedule_swap_proposal (proposed_by);

COMMENT ON TABLE app.vc_schedule_swap_proposal IS
    'EP-15 ST-15-2 STK_USER swap 제안 + Planner 1클릭 수용 (REQ-FUNC-VC-018)';
COMMENT ON COLUMN app.vc_schedule_swap_proposal.status IS
    'PROPOSED → ACCEPTED|REJECTED 상태 머신, audit trigger 가 자동 캡쳐';

-- =============================================================================
-- vc_schedule UNIQUE (machine, slot, date, rotation) 를 DEFERRABLE 로 변경
-- SwapProposalService.accept 가 atomic rotation_no swap 시 SET CONSTRAINTS DEFERRED 활용.
-- DB level invariant 는 그대로 유지 (transaction COMMIT 시점 enforce).
-- =============================================================================
ALTER TABLE app.vc_schedule
    DROP CONSTRAINT IF EXISTS uq_vc_schedule_slot_rotation;
ALTER TABLE app.vc_schedule
    ADD CONSTRAINT uq_vc_schedule_slot_rotation
        UNIQUE (machine_id, slot_position, production_date, rotation_no)
        DEFERRABLE INITIALLY IMMEDIATE;

-- =============================================================================
-- 상태 머신 trigger — PROPOSED → ACCEPTED|REJECTED 만 허용, 중복 resolution 차단
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_swap_proposal_transition()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN NEW;
    END IF;
    IF NOT (
        OLD.status = 'PROPOSED' AND NEW.status IN ('ACCEPTED','REJECTED')
    ) THEN
        RAISE EXCEPTION 'BR-X01 swap proposal invalid transition: % → % (proposal_id=%)',
            OLD.status, NEW.status, OLD.proposal_id;
    END IF;
    IF NEW.status IN ('ACCEPTED','REJECTED')
       AND (NEW.resolved_at IS NULL OR NEW.resolved_by IS NULL) THEN
        RAISE EXCEPTION 'swap proposal resolution 시 resolved_at + resolved_by 필수';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_swap_proposal_transition ON app.vc_schedule_swap_proposal;
CREATE TRIGGER trg_swap_proposal_transition
    BEFORE UPDATE ON app.vc_schedule_swap_proposal
    FOR EACH ROW EXECUTE FUNCTION app.enforce_swap_proposal_transition();
