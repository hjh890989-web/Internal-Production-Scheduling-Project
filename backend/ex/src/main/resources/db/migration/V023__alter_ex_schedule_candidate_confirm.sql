-- =============================================================================
-- V023__alter_ex_schedule_candidate_confirm.sql — TK-10-2 (EP-10 ST-10-2)
-- =============================================================================
-- EX candidate Confirmed 상태 게이트 — Planner role 명시적 확정 필수 (BR-X01).
-- VC 패턴 (V022) 재사용.
-- =============================================================================

ALTER TABLE app.ex_schedule_candidate
    ADD COLUMN IF NOT EXISTS confirmed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS confirmed_by  VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_ex_candidate_confirmed
    ON app.ex_schedule_candidate (confirmed_at) WHERE confirmed_at IS NOT NULL;

COMMENT ON COLUMN app.ex_schedule_candidate.confirmed_at IS
    'BR-X01 Planner 확정 시각 (Clock 주입, KST)';
COMMENT ON COLUMN app.ex_schedule_candidate.confirmed_by IS
    'BR-X01 Planner 사번 (RBAC ROLE_PLANNER)';

-- =============================================================================
-- 상태 전이 trigger — PENDING → READY → SCHEDULED → CONFIRMED|FAILED
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_ex_candidate_transition()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN NEW;
    END IF;

    -- 허용 전이 매트릭스
    IF NOT (
        (OLD.status = 'PENDING'    AND NEW.status IN ('READY','FAILED')) OR
        (OLD.status = 'READY'      AND NEW.status IN ('SCHEDULED','PENDING','FAILED')) OR
        (OLD.status = 'SCHEDULED'  AND NEW.status IN ('CONFIRMED','PENDING','FAILED')) OR
        (OLD.status = 'CONFIRMED'  AND NEW.status IN ('PENDING','FAILED'))     -- override / replan
    ) THEN
        RAISE EXCEPTION 'BR-X01 invalid EX candidate transition: % → % (ex_candidate_id=%)',
            OLD.status, NEW.status, OLD.ex_candidate_id;
    END IF;

    -- CONFIRMED 전이 시 confirmed_at/by 필수
    IF NEW.status = 'CONFIRMED' AND (NEW.confirmed_at IS NULL OR NEW.confirmed_by IS NULL) THEN
        RAISE EXCEPTION 'BR-X01 EX CONFIRMED 전이 시 confirmed_at + confirmed_by 필수';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ex_candidate_transition ON app.ex_schedule_candidate;
CREATE TRIGGER trg_ex_candidate_transition
    BEFORE UPDATE ON app.ex_schedule_candidate
    FOR EACH ROW EXECUTE FUNCTION app.enforce_ex_candidate_transition();
