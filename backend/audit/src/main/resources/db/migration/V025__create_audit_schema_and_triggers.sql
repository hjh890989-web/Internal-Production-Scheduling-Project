-- =============================================================================
-- V025__create_audit_schema_and_triggers.sql — TK-11-1-1 (EP-11 ST-11-1)
-- =============================================================================
-- 모든 mutation 자동 캡쳐 — vc_schedule / ex_schedule_candidate / order.
-- AOP @Auditable 가 SET_CONFIG('audit.actor', ..., true) 로 actor 주입,
-- trigger 가 current_setting 으로 fallback 'system'.
--
-- BR-X02 — audit 없는 mutation 100% 차단 (audit row 자동 생성).
-- NFR-SEC-004 — 3년 보존, schema 'audit' INSERT-only.
--
-- 주의 — TG_OP/OLD/NEW 는 trigger 함수 안에서만 사용 가능 → 각 테이블별 trigger 함수
-- 안에 INSERT 로직 인라인.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS audit;
COMMENT ON SCHEMA audit IS
    'EP-11 BR-X02 mutation audit (INSERT-only, REVOKE UPDATE/DELETE) — NFR-SEC-004 3년 보존';

-- =============================================================================
-- 통합 audit 테이블 — 1 row = 1 mutation
-- =============================================================================
CREATE TABLE IF NOT EXISTS audit.schedule_audit_log (
    audit_id        BIGSERIAL    PRIMARY KEY,
    table_name      VARCHAR(40)  NOT NULL,
    row_pk          VARCHAR(80)  NOT NULL,
    action          VARCHAR(10)  NOT NULL CHECK (action IN ('INSERT','UPDATE','DELETE')),
    old_row         JSONB,
    new_row         JSONB,
    actor           VARCHAR(40)  NOT NULL DEFAULT 'system',
    reason          TEXT,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_table_pk
    ON audit.schedule_audit_log (table_name, row_pk);
CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at
    ON audit.schedule_audit_log (occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor
    ON audit.schedule_audit_log (actor) WHERE actor <> 'system';

COMMENT ON TABLE audit.schedule_audit_log IS
    'EP-11 BR-X02 통합 mutation log — table_name 으로 source 구분';

-- =============================================================================
-- vc_schedule audit trigger
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_vc_schedule()
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
        'vc_schedule',
        COALESCE(NEW.vc_schedule_id, OLD.vc_schedule_id)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_vc_schedule ON app.vc_schedule;
CREATE TRIGGER trg_audit_vc_schedule
    AFTER INSERT OR UPDATE OR DELETE ON app.vc_schedule
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_vc_schedule();

-- =============================================================================
-- ex_schedule_candidate audit trigger
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_ex_candidate()
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
        'ex_schedule_candidate',
        COALESCE(NEW.ex_candidate_id, OLD.ex_candidate_id)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_ex_candidate ON app.ex_schedule_candidate;
CREATE TRIGGER trg_audit_ex_candidate
    AFTER INSERT OR UPDATE OR DELETE ON app.ex_schedule_candidate
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_ex_candidate();

-- =============================================================================
-- app.order audit trigger ("order" 는 예약어 — 쌍따옴표)
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_order()
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
        'order',
        COALESCE(NEW.order_id, OLD.order_id)::text,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_order ON app."order";
CREATE TRIGGER trg_audit_order
    AFTER INSERT OR UPDATE OR DELETE ON app."order"
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_order();
