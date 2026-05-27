-- =============================================================================
-- V045__sample_cleanup_function.sql — Sprint 19 EP-BETA-LAUNCH TK-BETA-1-1
-- =============================================================================
-- 베타 cutover 시점 99999-SAMPLE-* seed (V039 vc_schedule + V040 ex_candidate) 제거.
--
-- 설계: Flyway migration 은 함수 정의만 — 자동 발화 안 함 (auto-delete 위험 방지).
-- 운영자가 cutover runbook 의 명시적 단계에서 호출:
--   SELECT * FROM app.cleanup_99999_samples();
--
-- 반환: (table_name, deleted_count) — 운영자가 결과 확인 후 다음 단계 진행.
-- idempotent — 두 번 호출 시 두 번째는 deleted_count=0 (이미 삭제됨).
--
-- 정합:
--   V039 (vc_schedule 'hose_id LIKE 99999-SAMPLE-%') — Sprint 14 sample seed
--   V040 (ex_schedule_candidate sub-select from V039) — Sprint 15 sample seed
--
-- BR-X02 audit — 삭제 row 는 trg_audit_vc_schedule (V025) + 향후 trg_audit_ex_candidate 가
-- 자동 캡쳐 (audit.actor 는 함수 호출 actor 로 set 후 호출 권장).
-- =============================================================================

CREATE OR REPLACE FUNCTION app.cleanup_99999_samples()
RETURNS TABLE(table_name TEXT, deleted_count INT)
LANGUAGE plpgsql AS $$
DECLARE
    v_vc_count INT;
    v_ex_count INT;
BEGIN
    -- 1) vc_schedule sample (V039 origin)
    DELETE FROM app.vc_schedule WHERE hose_id LIKE '99999-SAMPLE-%';
    GET DIAGNOSTICS v_vc_count = ROW_COUNT;
    table_name := 'vc_schedule';
    deleted_count := v_vc_count;
    RETURN NEXT;

    -- 2) ex_schedule_candidate sample (V040 origin, V039 chain)
    DELETE FROM app.ex_schedule_candidate WHERE hose_id LIKE '99999-SAMPLE-%';
    GET DIAGNOSTICS v_ex_count = ROW_COUNT;
    table_name := 'ex_schedule_candidate';
    deleted_count := v_ex_count;
    RETURN NEXT;
END;
$$;

COMMENT ON FUNCTION app.cleanup_99999_samples() IS
    'Sprint 19 EP-BETA-LAUNCH cutover — 99999-SAMPLE-* seed 제거 (vc_schedule + ex_schedule_candidate). 운영자가 cutover runbook 의 명시적 단계에서 호출.';
