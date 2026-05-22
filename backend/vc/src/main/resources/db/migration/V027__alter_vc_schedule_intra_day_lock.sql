-- =============================================================================
-- V027__alter_vc_schedule_intra_day_lock.sql — TK-13-1+3+4 (EP-13 ST-13-1·3·4)
-- =============================================================================
-- BR-V07 (v1.1 재정의) — 당일 락:
--   "당일 투입 (가류기, 슬롯, 품번)은 rotation 1~18 동일 앵글 연속 운전.
--    앵글 교체는 일말~다음 영업일 사이에만 허용, 일중 교체는 hard 위반"
-- (machine_id, slot_position, production_date) 단위로 angle_id 단일성 강제.
-- override 시 override_reason 강제 입력 — audit 자동 캡쳐 (V025 trigger).
-- =============================================================================

-- =============================================================================
-- 1) Override 컬럼 — 사유 + actor (EP-13 ST-13-4)
-- =============================================================================
ALTER TABLE app.vc_schedule
    ADD COLUMN IF NOT EXISTS override_reason TEXT,
    ADD COLUMN IF NOT EXISTS override_by     VARCHAR(40);

COMMENT ON COLUMN app.vc_schedule.override_reason IS
    'BR-V07 일중 앵글 교체 override 사유 (NULL = 일반, 비-NULL = override)';
COMMENT ON COLUMN app.vc_schedule.override_by IS
    'BR-V07 override actor (ROLE_PLANNER 사번)';

-- =============================================================================
-- 2) 마이그레이션 사전 점검 SQL (Sprint 4 안전성)
--    기존 vc_schedule row 중 (machine, slot, date) 안에 다른 angle_id 가 있다면
--    이 마이그레이션은 트리거 활성 시 다음 INSERT/UPDATE 부터만 차단 (기존 row 무영향)
-- =============================================================================
DO $$
DECLARE
    v_violations INT;
BEGIN
    SELECT COUNT(*) INTO v_violations FROM (
        SELECT machine_id, slot_position, production_date
        FROM app.vc_schedule
        GROUP BY machine_id, slot_position, production_date
        HAVING COUNT(DISTINCT angle_id) > 1
    ) AS dups;
    IF v_violations > 0 THEN
        RAISE NOTICE 'V027 사전 점검: BR-V07 위반 기존 row % 슬롯 — 트리거 활성 후 신규 INSERT/UPDATE 부터 차단',
            v_violations;
    END IF;
END $$;

-- =============================================================================
-- 3) BR-V07 일중 락 trigger — (machine, slot, date) 안 다른 angle_id 차단
--    override_reason 비-NULL 이면 통과 (사용자 명시 override)
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_vc_intra_day_lock()
RETURNS TRIGGER AS $$
DECLARE
    v_existing_angle VARCHAR(40);
BEGIN
    -- 같은 slot 의 기존 angle_id 조회 (자기 자신 제외)
    SELECT DISTINCT angle_id INTO v_existing_angle
    FROM app.vc_schedule
    WHERE machine_id      = NEW.machine_id
      AND slot_position   = NEW.slot_position
      AND production_date = NEW.production_date
      AND vc_schedule_id <> NEW.vc_schedule_id
      AND angle_id        <> NEW.angle_id
    LIMIT 1;

    IF v_existing_angle IS NOT NULL THEN
        -- override_reason 강제
        IF NEW.override_reason IS NULL OR length(trim(NEW.override_reason)) = 0 THEN
            RAISE EXCEPTION 'BR-V07 일중 앵글 교체 차단: machine=% slot=% date=% existing_angle=% new_angle=% (override_reason 강제 필수)',
                NEW.machine_id, NEW.slot_position, NEW.production_date,
                v_existing_angle, NEW.angle_id;
        END IF;
        -- override_by 필수
        IF NEW.override_by IS NULL OR length(trim(NEW.override_by)) = 0 THEN
            RAISE EXCEPTION 'BR-V07 override_by (actor) 강제 필수: machine=% slot=% date=%',
                NEW.machine_id, NEW.slot_position, NEW.production_date;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_intra_day_lock ON app.vc_schedule;
CREATE TRIGGER trg_vc_intra_day_lock
    BEFORE INSERT OR UPDATE ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION app.enforce_vc_intra_day_lock();

COMMENT ON FUNCTION app.enforce_vc_intra_day_lock() IS
    'EP-13 ST-13-1 BR-V07 일중 앵글 교체 차단 (override_reason+actor 강제)';
