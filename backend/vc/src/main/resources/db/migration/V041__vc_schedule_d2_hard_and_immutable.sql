-- =============================================================================
-- V041__vc_schedule_d2_hard_and_immutable.sql — Sprint 16 EP-CONFIRM
--   TK-CONFIRM-1-1 (BR-X07 D-2 hard) + TK-CONFIRM-3-1 (BR-X01 CONFIRMED immutable)
-- =============================================================================
-- BR-X07 (확정 게이트 D-2 hard): production_date - CURRENT_DATE < 2 일 → 신규 추가 차단.
--   D-0 (당일, BR-V07 일중 락) · D-1 (전일) · D-2 미만 → INSERT 모두 RAISE.
--   D-3 (= 2일 이상) 이상만 신규 row 허용 — VC 시뮬뷰 draft 진입 차단의 hard 가드.
--
-- BR-X01 (확정 게이트 immutable): status='CONFIRMED' row 의 도메인 컬럼 수정 차단.
--   V022 트리거가 status 전이 (CONFIRMED → DONE / CANDIDATE) 는 이미 강제.
--   본 트리거는 status 미변경 UPDATE 중 도메인 컬럼 (qty, angle, slot, machine, date,
--   hose, rotation) 변조 차단. updated_at 만 변경되는 housekeeping 은 허용.
-- =============================================================================

-- =============================================================================
-- 1) BR-X07 D-2 hard — BEFORE INSERT 만 (UPDATE 는 production_date 미변경 허용)
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_vc_schedule_d2_hard()
RETURNS TRIGGER AS $$
DECLARE
    v_days_to_production INT;
BEGIN
    v_days_to_production := NEW.production_date - CURRENT_DATE;
    IF v_days_to_production < 2 THEN
        RAISE EXCEPTION 'BR-X07 D-2 hard 제약: production_date=% 가 현재 (D-0=%) 기준 % 일 — 2일 미만이므로 신규 추가 차단',
            NEW.production_date, CURRENT_DATE, v_days_to_production
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_schedule_d2_hard ON app.vc_schedule;
CREATE TRIGGER trg_vc_schedule_d2_hard
    BEFORE INSERT ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION app.enforce_vc_schedule_d2_hard();

COMMENT ON FUNCTION app.enforce_vc_schedule_d2_hard() IS
    'Sprint 16 BR-X07 D-2 hard 제약 — production_date - CURRENT_DATE < 2 신규 차단';

-- =============================================================================
-- 2) BR-X01 CONFIRMED immutable — BEFORE UPDATE, status 미변경 시 도메인 변조 차단
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_vc_schedule_confirmed_immutable()
RETURNS TRIGGER AS $$
BEGIN
    -- CONFIRMED 유지 UPDATE — 도메인 컬럼 어느 하나라도 다르면 차단
    IF OLD.status = 'CONFIRMED' AND NEW.status = 'CONFIRMED' THEN
        IF OLD.hose_id          IS DISTINCT FROM NEW.hose_id
        OR OLD.machine_id       IS DISTINCT FROM NEW.machine_id
        OR OLD.slot_position    IS DISTINCT FROM NEW.slot_position
        OR OLD.production_date  IS DISTINCT FROM NEW.production_date
        OR OLD.rotation_no      IS DISTINCT FROM NEW.rotation_no
        OR OLD.angle_id         IS DISTINCT FROM NEW.angle_id
        OR OLD.planned_qty      IS DISTINCT FROM NEW.planned_qty THEN
            RAISE EXCEPTION 'BR-X01 CONFIRMED 스케줄 immutable: vc_schedule_id=% — 도메인 컬럼 수정 차단 (status 전이 DONE/CANDIDATE 만 허용)',
                OLD.vc_schedule_id
                USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_schedule_confirmed_immutable ON app.vc_schedule;
CREATE TRIGGER trg_vc_schedule_confirmed_immutable
    BEFORE UPDATE ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION app.enforce_vc_schedule_confirmed_immutable();

COMMENT ON FUNCTION app.enforce_vc_schedule_confirmed_immutable() IS
    'Sprint 16 BR-X01 CONFIRMED 스케줄 도메인 컬럼 변조 차단 (status 전이만 허용)';
