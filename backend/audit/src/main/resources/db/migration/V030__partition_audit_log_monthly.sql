-- =============================================================================
-- V030__partition_audit_log_monthly.sql — TK-41-3-1 (Sprint 6 인프라)
-- =============================================================================
-- audit.schedule_audit_log 월별 RANGE 파티셔닝 — NFR-SEC-004 3년 보존 + 인덱스 효율.
--
-- 전략:
--   - 기존 테이블 (audit.schedule_audit_log) → 파티셔닝 테이블로 재구성
--   - 자식 partition 36개 (2026-01 ~ 2028-12 — Phase 2 운영 기간 + 3년 보존)
--   - DEFAULT partition — 미래/과거 fallback
--   - 인덱스 (table_name, row_pk) + (occurred_at) 자동 상속
--
-- 주의 — partitioned table 은 기존 PRIMARY KEY 가 partition key (occurred_at) 포함 필요.
-- 따라서 PK 를 (audit_id, occurred_at) 복합으로 변경 + UNIQUE INDEX 유지.
-- =============================================================================

-- 0) 기존 데이터 백업 (테스트 환경 — Testcontainers 신규 DB 는 빈 테이블)
CREATE TABLE IF NOT EXISTS audit.schedule_audit_log_backup AS
    SELECT * FROM audit.schedule_audit_log;

-- 1) 트리거/제약 임시 제거 → 테이블 재생성
DROP TRIGGER IF EXISTS trg_block_audit_mutation ON audit.schedule_audit_log;
DROP TRIGGER IF EXISTS trg_block_audit_truncate ON audit.schedule_audit_log;

ALTER TABLE app.vc_schedule          DISABLE TRIGGER trg_audit_vc_schedule;
ALTER TABLE app.ex_schedule_candidate DISABLE TRIGGER trg_audit_ex_candidate;
ALTER TABLE app."order"              DISABLE TRIGGER trg_audit_order;

DROP TABLE audit.schedule_audit_log;

-- 2) 파티셔닝 테이블 신규 — PK 에 occurred_at 포함 (PostgreSQL 요건)
CREATE TABLE audit.schedule_audit_log (
    audit_id        BIGSERIAL    NOT NULL,
    table_name      VARCHAR(40)  NOT NULL,
    row_pk          VARCHAR(80)  NOT NULL,
    action          VARCHAR(10)  NOT NULL CHECK (action IN ('INSERT','UPDATE','DELETE')),
    old_row         JSONB,
    new_row         JSONB,
    actor           VARCHAR(40)  NOT NULL DEFAULT 'system',
    reason          TEXT,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (audit_id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE INDEX idx_audit_log_table_pk
    ON audit.schedule_audit_log (table_name, row_pk);
CREATE INDEX idx_audit_log_occurred_at
    ON audit.schedule_audit_log (occurred_at);
CREATE INDEX idx_audit_log_actor
    ON audit.schedule_audit_log (actor) WHERE actor <> 'system';

-- 3) 36 월별 partitions (2026-01 ~ 2028-12)
DO $$
DECLARE
    y INT;
    m INT;
    start_ts TEXT;
    end_ts TEXT;
    pname TEXT;
BEGIN
    FOR y IN 2026..2028 LOOP
        FOR m IN 1..12 LOOP
            pname    := format('schedule_audit_log_y%sm%s', y, lpad(m::text, 2, '0'));
            start_ts := format('%s-%s-01', y, lpad(m::text, 2, '0'));
            IF m = 12 THEN
                end_ts := format('%s-01-01', y + 1);
            ELSE
                end_ts := format('%s-%s-01', y, lpad((m + 1)::text, 2, '0'));
            END IF;
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS audit.%I PARTITION OF audit.schedule_audit_log '
                || 'FOR VALUES FROM (%L) TO (%L)',
                pname, start_ts, end_ts);
        END LOOP;
    END LOOP;
END $$;

-- 4) DEFAULT partition — fallback (2025 이전·2029 이후)
CREATE TABLE IF NOT EXISTS audit.schedule_audit_log_default
    PARTITION OF audit.schedule_audit_log DEFAULT;

-- 5) 백업에서 복원
INSERT INTO audit.schedule_audit_log (
    audit_id, table_name, row_pk, action, old_row, new_row, actor, reason, occurred_at
) SELECT audit_id, table_name, row_pk, action, old_row, new_row, actor, reason, occurred_at
  FROM audit.schedule_audit_log_backup;

DROP TABLE audit.schedule_audit_log_backup;

-- 6) V026 immutability 트리거 재생성
CREATE TRIGGER trg_block_audit_mutation
    BEFORE UPDATE OR DELETE ON audit.schedule_audit_log
    FOR EACH ROW EXECUTE FUNCTION audit.fn_block_mutation();

CREATE TRIGGER trg_block_audit_truncate
    BEFORE TRUNCATE ON audit.schedule_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit.fn_block_truncate();

-- 7) V025 audit trigger 재활성
ALTER TABLE app.vc_schedule          ENABLE TRIGGER trg_audit_vc_schedule;
ALTER TABLE app.ex_schedule_candidate ENABLE TRIGGER trg_audit_ex_candidate;
ALTER TABLE app."order"              ENABLE TRIGGER trg_audit_order;

-- 8) 권한 재적용 (V026 REVOKE 복원)
REVOKE UPDATE, DELETE, TRUNCATE ON audit.schedule_audit_log FROM PUBLIC;
GRANT  SELECT  ON audit.schedule_audit_log TO audit_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT ON TABLES TO audit_reader;

COMMENT ON TABLE audit.schedule_audit_log IS
    'EP-11 BR-X02 통합 mutation log — 월별 RANGE 파티셔닝 (V030, NFR-SEC-004 3년 보존)';
