-- =============================================================================
-- V038 — Sprint 12 후속 hotfix BR-V12/V13 master.product_priority + kd_order audit trigger
-- =============================================================================
-- Sprint 7 V033 (PRODUCT_PRIORITY + KD_ORDER schema) 도입 시 audit trigger 누락 — BR-X02
-- (mutation audit 강제) 위반 closure. Sprint 9 V035 (capacity_overflow_request) 와 동일 패턴.
--
-- 발견 시점 — Sprint 12 EP-MASTER-UI ST-MASTER-3·4 IT_OPS CRUD 검증 중 audit_log.actor 미기록
-- (AOP @Auditable 가 set_config 정상 호출하지만 trigger 부재로 row 미생성).
--
-- 적용 작업 — ProductPriorityAdminService + KdOrderAdminService 의 create/update/delete.
-- =============================================================================

-- =============================================================================
-- master.product_priority audit trigger
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_product_priority()
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
        'product_priority',
        COALESCE(NEW.hose_id, OLD.hose_id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_product_priority ON master.product_priority;
CREATE TRIGGER trg_audit_product_priority
    AFTER INSERT OR UPDATE OR DELETE ON master.product_priority
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_product_priority();

-- =============================================================================
-- master.kd_order audit trigger
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_kd_order()
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
        'kd_order',
        COALESCE(NEW.kd_order_id, OLD.kd_order_id)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_kd_order ON master.kd_order;
CREATE TRIGGER trg_audit_kd_order
    AFTER INSERT OR UPDATE OR DELETE ON master.kd_order
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_kd_order();
