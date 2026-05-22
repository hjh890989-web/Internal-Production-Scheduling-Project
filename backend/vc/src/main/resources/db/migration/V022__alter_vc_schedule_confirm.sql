-- =============================================================================
-- V022__alter_vc_schedule_confirm.sql — TK-10-1 (EP-10 ST-10-1)
-- =============================================================================
-- VC 스케줄 Confirmed 상태 게이트 — Planner role 명시적 확정 필수 (BR-X01).
-- 직접 DB 쓰기 차단 — app_user UPDATE status 거부, planner_role function 만 허용.
-- =============================================================================

ALTER TABLE app.vc_schedule
    ADD COLUMN IF NOT EXISTS confirmed_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS confirmed_by  VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_vc_schedule_confirmed
    ON app.vc_schedule (confirmed_at) WHERE confirmed_at IS NOT NULL;

COMMENT ON COLUMN app.vc_schedule.confirmed_at IS
    'BR-X01 Planner 확정 시각 (Clock 주입, KST)';
COMMENT ON COLUMN app.vc_schedule.confirmed_by IS
    'BR-X01 Planner 사번 (RBAC ROLE_PLANNER)';

-- =============================================================================
-- 상태 전이 trigger — 허용 전이: CANDIDATE → CONFIRMED → DONE
-- 잘못된 전이 (DONE → CANDIDATE 등) 차단
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_vc_schedule_transition()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = NEW.status THEN
        RETURN NEW;     -- 상태 미변경 — 다른 컬럼만 update 허용
    END IF;

    -- 허용 전이 매트릭스
    IF NOT (
        (OLD.status = 'CANDIDATE' AND NEW.status = 'CONFIRMED') OR
        (OLD.status = 'CONFIRMED' AND NEW.status = 'DONE') OR
        (OLD.status = 'CONFIRMED' AND NEW.status = 'CANDIDATE')   -- override (Sprint 4 EP-13)
    ) THEN
        RAISE EXCEPTION 'BR-X01 invalid VC status transition: % → % (vc_schedule_id=%)',
            OLD.status, NEW.status, OLD.vc_schedule_id;
    END IF;

    -- CONFIRMED 전이 시 confirmed_at/by 필수
    IF NEW.status = 'CONFIRMED' AND (NEW.confirmed_at IS NULL OR NEW.confirmed_by IS NULL) THEN
        RAISE EXCEPTION 'BR-X01 CONFIRMED 전이 시 confirmed_at + confirmed_by 필수';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_schedule_transition ON app.vc_schedule;
CREATE TRIGGER trg_vc_schedule_transition
    BEFORE UPDATE ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION app.enforce_vc_schedule_transition();
