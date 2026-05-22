-- =============================================================================
-- V026__revoke_audit_immutability.sql — TK-11-2-1 (EP-11 ST-11-2)
-- =============================================================================
-- audit 데이터 변조 차단 — UPDATE/DELETE/TRUNCATE 거부 (NFR-SEC-004).
-- (1) 권한 REVOKE — Flyway 사용자 외 모든 role 에서 변조 권한 회수.
-- (2) BEFORE UPDATE/DELETE/TRUNCATE 트리거 — owner/superuser 도 차단.
--     ※ Postgres 는 BEFORE TRUNCATE 가 statement-level 만 가능 (per-row 아님).
-- =============================================================================

-- =============================================================================
-- 1) audit_reader role (조회 전용)
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_reader') THEN
        CREATE ROLE audit_reader;
    END IF;
END $$;

GRANT USAGE  ON SCHEMA audit TO audit_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO audit_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT ON TABLES TO audit_reader;

-- =============================================================================
-- 2) REVOKE — PUBLIC 으로부터 UPDATE/DELETE/TRUNCATE 모두 회수
--    INSERT 는 trigger 가 자동 발행하므로 app_user 는 유지 (또는 SECURITY DEFINER 함수 경로)
-- =============================================================================
REVOKE UPDATE, DELETE, TRUNCATE
    ON ALL TABLES IN SCHEMA audit
    FROM PUBLIC;

-- =============================================================================
-- 3) BEFORE UPDATE/DELETE 트리거 — owner 도 차단 (변조 시 RAISE)
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_block_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'NFR-SEC-004 audit row 변조 금지 (table=%, op=%)',
        TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_block_audit_mutation ON audit.schedule_audit_log;
CREATE TRIGGER trg_block_audit_mutation
    BEFORE UPDATE OR DELETE ON audit.schedule_audit_log
    FOR EACH ROW EXECUTE FUNCTION audit.fn_block_mutation();

-- TRUNCATE 차단 (statement-level)
CREATE OR REPLACE FUNCTION audit.fn_block_truncate()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'NFR-SEC-004 audit TRUNCATE 금지 (table=%)', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_block_audit_truncate ON audit.schedule_audit_log;
CREATE TRIGGER trg_block_audit_truncate
    BEFORE TRUNCATE ON audit.schedule_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit.fn_block_truncate();

COMMENT ON FUNCTION audit.fn_block_mutation() IS
    'EP-11 ST-11-2 NFR-SEC-004 audit row 변조 차단 (UPDATE/DELETE 거부)';
COMMENT ON FUNCTION audit.fn_block_truncate() IS
    'EP-11 ST-11-2 NFR-SEC-004 audit TRUNCATE 차단';
