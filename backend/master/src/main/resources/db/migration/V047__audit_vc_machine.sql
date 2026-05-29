-- =============================================================================
-- V047 — Sprint 21 ST-CRUD-1 master.vc_machine audit trigger (BR-X02)
-- =============================================================================
-- VcMachineAdminService create/update/deactivate 호출 시
-- @Auditable AOP 가 set_config('audit.actor'/'audit.reason') 을 주입하면
-- 아래 trigger 가 audit.schedule_audit_log 에 row 를 기록한다.
-- V038 (product_priority / kd_order) 와 동일 패턴.
-- =============================================================================

CREATE OR REPLACE FUNCTION audit.fn_audit_vc_machine()
RETURNS TRIGGER AS $$
DECLARE
    v_actor  VARCHAR(40);
    v_reason TEXT;
BEGIN
    v_actor  := COALESCE(NULLIF(current_setting('audit.actor',  true), ''), 'system');
    v_reason := NULLIF(current_setting('audit.reason', true), '');
    INSERT INTO audit.schedule_audit_log (
        table_name, row_pk, action, old_row, new_row, actor, reason
    ) VALUES (
        'vc_machine',
        COALESCE(NEW.machine_id, OLD.machine_id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_vc_machine ON master.vc_machine;
CREATE TRIGGER trg_audit_vc_machine
    AFTER INSERT OR UPDATE OR DELETE ON master.vc_machine
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_vc_machine();
