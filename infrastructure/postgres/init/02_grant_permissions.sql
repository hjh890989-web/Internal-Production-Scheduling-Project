-- =============================================================================
-- 02_grant_permissions.sql — 스키마별 권한 분리
-- =============================================================================
-- app_user      → app RW, master SELECT, audit INSERT (애플리케이션 계정)
-- auditor       → audit SELECT only                  (감사·임원)
-- master_admin  → master ALL                         (BR-X05 dual-review 후)
-- =============================================================================

-- ---------- app_user (애플리케이션 일반 계정) ----------
GRANT USAGE ON SCHEMA app, master, audit TO app_user;

-- app 스키마: 모든 권한
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA app TO app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA app TO app_user;

-- master 스키마: SELECT only (수정은 master_admin 통해서)
GRANT SELECT ON ALL TABLES IN SCHEMA master TO app_user;

-- audit 스키마: INSERT only (REQ-NF-SEC-004 불변성 — UPDATE/DELETE 금지)
GRANT INSERT ON ALL TABLES    IN SCHEMA audit TO app_user;
GRANT USAGE  ON ALL SEQUENCES IN SCHEMA audit TO app_user;

-- ---------- auditor (감사·임원 — audit 조회만) ----------
GRANT USAGE  ON SCHEMA audit TO auditor;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO auditor;

-- ---------- master_admin (BR-X05 dual-review 통과 후 활성) ----------
GRANT USAGE  ON SCHEMA master TO master_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA master TO master_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA master TO master_admin;

-- ---------- 기본 권한 (향후 생성될 테이블 자동 적용) ----------
ALTER DEFAULT PRIVILEGES IN SCHEMA app    GRANT ALL          ON TABLES    TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA app    GRANT ALL          ON SEQUENCES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA master GRANT SELECT       ON TABLES    TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit  GRANT INSERT       ON TABLES    TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit  GRANT USAGE        ON SEQUENCES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit  GRANT SELECT       ON TABLES    TO auditor;
ALTER DEFAULT PRIVILEGES IN SCHEMA master GRANT ALL          ON TABLES    TO master_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA master GRANT ALL          ON SEQUENCES TO master_admin;

-- ※ audit UPDATE/DELETE 차단 트리거는 EP-11 ST-11-2 (Sprint 2) 에서 추가.
--   본 Task 시점 권한 부여만으로도 app_user는 audit UPDATE/DELETE 시도 시 permission denied.
