-- =============================================================================
-- V047 — Sprint 21 ST-CRUD-3 BR-X02 vc_constraint audit trigger
-- =============================================================================
-- VcConstraintAdminService (IT_OPS write) mutation 시 audit_log 기록.
-- pattern: V038 (product_priority / kd_order) 동일.
-- =============================================================================

CREATE OR REPLACE FUNCTION audit.fn_audit_vc_constraint()
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
        'vc_constraint',
        COALESCE(NEW.hose_id, OLD.hose_id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_vc_constraint ON master.vc_constraint;
CREATE TRIGGER trg_audit_vc_constraint
    AFTER INSERT OR UPDATE OR DELETE ON master.vc_constraint
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_vc_constraint();
