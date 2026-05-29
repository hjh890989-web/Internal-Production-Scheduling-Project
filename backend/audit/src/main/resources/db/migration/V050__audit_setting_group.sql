-- =============================================================================
-- V048__audit_setting_group.sql — Sprint 21 ST-CRUD-2 BR-X02 audit trigger
-- =============================================================================
-- master.setting_group 의 INSERT/UPDATE/DELETE 를 audit.schedule_audit_log 에 기록.
-- @Auditable AOP 가 set_config('audit.actor'/'audit.reason', true) 주입 후 trigger 수신.
-- 동일 패턴: V038__audit_product_priority_and_kd_order.sql
-- =============================================================================

CREATE OR REPLACE FUNCTION audit.fn_audit_setting_group()
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
        'setting_group',
        COALESCE(NEW.group_number, OLD.group_number)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_setting_group ON master.setting_group;
CREATE TRIGGER trg_audit_setting_group
    AFTER INSERT OR UPDATE OR DELETE ON master.setting_group
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_setting_group();
