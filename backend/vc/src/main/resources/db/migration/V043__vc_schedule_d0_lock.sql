-- =============================================================================
-- V043__vc_schedule_d0_lock.sql — Sprint 17 EP-DAY-LOCK TK-DAY-LOCK-1-1
-- =============================================================================
-- BR-V07 (강화): 당일 (D-0, production_date == CURRENT_DATE) row UPDATE 차단.
--   override 경로 (BR-V07 일중 앵글 교체) 만 예외 — NEW.override_reason 가 OLD 와 다르면 통과.
--
-- 정합:
--   V027 trg_vc_intra_day_lock — (machine, slot, date) 단위 다른 angle_id 차단 (override 시 reason+by 강제)
--   V041 trg_vc_schedule_confirmed_immutable — CONFIRMED row 도메인 변조 차단
--   V043 (본): production_date == 오늘 row 의 모든 UPDATE 차단 (BR-V07 hard 강화)
--
-- 비고: confirm 액션 (CANDIDATE → CONFIRMED) 도 D-0 row 에서는 차단 — BR-X01 "D-2~D-1 만 수정 가능"
-- 정합. 실 운영 D-0 row 는 이미 CONFIRMED 상태일 것 (Sprint 16 정책 baseline).
-- =============================================================================

CREATE OR REPLACE FUNCTION app.enforce_vc_schedule_d0_lock()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.production_date = CURRENT_DATE THEN
        -- override 경로 — override_reason 가 새로 set 또는 변경된 경우만 통과
        IF NEW.override_reason IS DISTINCT FROM OLD.override_reason
           AND NEW.override_reason IS NOT NULL
           AND length(trim(NEW.override_reason)) > 0
           AND NEW.override_by IS NOT NULL
           AND length(trim(NEW.override_by)) > 0 THEN
            RETURN NEW;
        END IF;
        RAISE EXCEPTION 'BR-V07 D-0 (당일) 락: production_date=% (오늘) row 수정 차단 — 일중 교체는 override_reason+override_by 갱신 필수',
            OLD.production_date
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_schedule_d0_lock ON app.vc_schedule;
CREATE TRIGGER trg_vc_schedule_d0_lock
    BEFORE UPDATE ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION app.enforce_vc_schedule_d0_lock();

COMMENT ON FUNCTION app.enforce_vc_schedule_d0_lock() IS
    'Sprint 17 BR-V07 D-0 락 강화 — production_date == CURRENT_DATE row UPDATE 차단 (override_reason 경로 예외)';
