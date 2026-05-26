-- =============================================================================
-- V035 — Sprint 8 후속 hotfix BR-V12 capacity_overflow_request audit trigger
-- =============================================================================
-- Sprint 8 V034 capacity_overflow_request 도입 시 audit trigger 누락 — BR-X02
-- (mutation audit 강제) 위반 closure. V025 패턴 동일 (vc_schedule/ex_schedule_candidate/
-- order audit trigger 와 같은 형태).
--
-- AOP @Auditable (AuditableAspect) 가 set_config('audit.actor', ...) 주입 →
-- 본 trigger 가 current_setting 으로 pickup. AOP 적용 안 된 경로 — 'system' fallback.
--
-- 적용 작업 — enqueue (INSERT) · accept/reject (UPDATE) · Sprint 9 auto-expire (UPDATE).
-- =============================================================================

CREATE OR REPLACE FUNCTION audit.fn_audit_capacity_overflow_request()
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
        'capacity_overflow_request',
        COALESCE(NEW.request_id, OLD.request_id)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_capacity_overflow_request ON app.capacity_overflow_request;
CREATE TRIGGER trg_audit_capacity_overflow_request
    AFTER INSERT OR UPDATE OR DELETE ON app.capacity_overflow_request
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_capacity_overflow_request();
