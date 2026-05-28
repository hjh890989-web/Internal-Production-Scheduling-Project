-- =============================================================================
-- V046__fix_duplicate_ic_machine.sql — Sprint 19 hotfix
-- =============================================================================
-- Sprint 14 V039 가 'IC-01' 대신 'IC' (legacy 표기) 로 vc_machine + 99999-SAMPLE-C*
-- vc_schedule INSERT — V009 (Sprint 1) seed 의 'IC-01' 과 중복 머신 발생.
-- 결과: CapacityLedger 가 5+1=6 머신 카운트 → 684 cell (expected) 대신 792 cell.
--
-- Fix:
--   1) vc_schedule 의 machine_id='IC' row → 'IC-01' UPDATE (FK 유지)
--      (99999-SAMPLE-C* 시드 보존 — Sprint 19 V045 cleanup_99999_samples() 가 명시 호출 시 제거)
--   2) master.vc_machine WHERE machine_id='IC' DELETE
--
-- Side effect: CapacityLedger 정상 (5 머신 = 4 LP + 1 IC-01 = 684 cell).
-- =============================================================================

-- 1) vc_schedule machine_id 'IC' → 'IC-01' (UNIQUE 제약 충돌 가능성 — UNIQUE 는
--    (machine, slot, date, rotation) 이므로 IC 와 IC-01 같은 slot/date/rot 동시 점유 시 충돌.
--    V039 sample 의 (IC,slot=1,date,rot=1) row 3건 — IC-01 에 동일 slot 점유 row 없음 (V009 seed
--    는 vc_schedule 없음). 따라서 UPDATE 안전.
--    BR-V07 D-0 lock trigger 차단 회피 — V039 sample 일부가 CURRENT_DATE row 일 수 있어 일시 비활성.
ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_schedule_d0_lock;
ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_schedule_confirmed_immutable;
ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_intra_day_lock;
UPDATE app.vc_schedule SET machine_id = 'IC-01' WHERE machine_id = 'IC';
ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_intra_day_lock;
ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_schedule_confirmed_immutable;
ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_schedule_d0_lock;

-- 2) 중복 vc_machine row 삭제
DELETE FROM master.vc_machine WHERE machine_id = 'IC';

COMMENT ON COLUMN master.vc_machine.machine_id IS
    'Sprint 1 V009 seed: LP-01~04 + IC-01 정본. Sprint 14 V039 의 ''IC'' alias 는 Sprint 19 V046 hotfix 에서 제거.';
