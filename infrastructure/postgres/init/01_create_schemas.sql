-- =============================================================================
-- 01_create_schemas.sql — 3 스키마 + role 분리 (SAD §6.1.1)
-- =============================================================================
-- app    : 운영 데이터 (PRODUCT·ORDER·VC_SCHEDULE·EX_SCHEDULE 등) — Sprint 1+ Flyway 마이그레이션
-- audit  : 감사 로그 (ORDER_CHANGE·VC_AUDIT·EX_AUDIT) — INSERT only, ≥3년 보존 (REQ-NF-SEC-004)
-- master : 마스터 데이터 (VC_CONSTRAINT·EX_CONSTRAINT·VC_HOSE_RULE 등) — dual-review (BR-X05)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS master;

COMMENT ON SCHEMA app    IS '운영 데이터 (PRODUCT·ORDER·VC_SCHEDULE·EX_SCHEDULE) — app_user RW';
COMMENT ON SCHEMA audit  IS '감사 로그 — INSERT only, ≥3년 보존 (REQ-NF-SEC-004), auditor role SELECT only';
COMMENT ON SCHEMA master IS '마스터 데이터 (VC_CONSTRAINT·EX_CONSTRAINT·VC_HOSE_RULE) — master_admin dual-review (BR-X05)';

-- Role 분리 (REQ-NF-SEC-004 audit 불변성)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auditor') THEN
        CREATE ROLE auditor NOLOGIN;
        COMMENT ON ROLE auditor IS 'audit 스키마 SELECT only — 감사·임원 조회 전용';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'master_admin') THEN
        CREATE ROLE master_admin NOLOGIN;
        COMMENT ON ROLE master_admin IS 'master 스키마 UPDATE 권한 — BR-X05 dual-review 후 적용';
    END IF;
END $$;

-- KST 기본 timezone (BR-X04, REQ-FUNC-CO-007)
-- POSTGRES_DB는 환경 변수, 기본값은 'scheduling'.
ALTER DATABASE scheduling SET timezone TO 'Asia/Seoul';
