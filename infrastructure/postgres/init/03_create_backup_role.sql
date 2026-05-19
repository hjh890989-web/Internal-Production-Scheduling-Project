-- =============================================================================
-- 03_create_backup_role.sql — pg_basebackup 전용 role (TK-33-2-1)
-- =============================================================================
-- pg_basebackup 은 REPLICATION 권한 필요. app_user 와 분리 — 최소 권한 원칙.
-- DEV/STG/PROD 모두 동일. PROD 비밀번호는 vault.
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'backup_user') THEN
        CREATE ROLE backup_user
            LOGIN
            REPLICATION
            CONNECTION LIMIT 2
            PASSWORD 'Dev_Backup_2026';
        COMMENT ON ROLE backup_user IS
            'pg_basebackup + WAL streaming 전용 — TK-33-2-1. REPLICATION + LOGIN only.';
    END IF;
END $$;

-- pg_hba.conf 항목은 컨테이너 기본 (host all all 0.0.0.0/0 scram-sha-256) 으로 처리.
-- PROD: pg_hba.conf 에 별도 'host replication backup_user 사내CIDR/24 scram-sha-256' 권장.
