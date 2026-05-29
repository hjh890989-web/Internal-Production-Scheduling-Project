-- =============================================================================
-- V053__audit_partition_maintenance_fn.sql — Sprint 22 ST-SEC-1 TK-SEC-3-1
-- =============================================================================
-- audit.schedule_audit_log 월별 partition rolling-window 유지 함수 (NFR-SEC-004).
--
-- 배경:
--   - V030 이 2026m01 ~ 2028m12 (36개) partition + DEFAULT 사전 생성.
--   - 2029-01 부터는 DEFAULT partition fallback → 비대화 + 인덱스 효율 저하.
--   - 본 함수 + PartitionMaintenanceScheduler (@Scheduled 매월 25일) 로 향후 12개월 사전 확보.
--
-- 불변성 (NFR-SEC-004):
--   - BEFORE UPDATE/DELETE ROW 트리거 (trg_block_audit_mutation, V030) 는 partitioned
--     parent 에 정의 → PostgreSQL 11+ 에서 신규 자식 partition 에 자동 상속. 별도 재생성 불필요.
--   - SELECT 권한은 V030 의 ALTER DEFAULT PRIVILEGES (audit_reader) 로 신규 table 자동 부여.
--   - 본 함수는 추가 방어로 PUBLIC 의 UPDATE/DELETE/TRUNCATE REVOKE.
--
-- idempotent — 이미 존재하는 월은 skip (이름 기준 pg_class lookup). 동시 cron 재실행 안전.
-- =============================================================================

CREATE OR REPLACE FUNCTION audit.ensure_month_partition(target DATE)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    y         INT  := EXTRACT(YEAR  FROM target);
    m         INT  := EXTRACT(MONTH FROM target);
    pname     TEXT;
    start_ts  TEXT;
    end_ts    TEXT;
BEGIN
    pname    := format('schedule_audit_log_y%sm%s', y, lpad(m::text, 2, '0'));
    start_ts := format('%s-%s-01', y, lpad(m::text, 2, '0'));
    IF m = 12 THEN
        end_ts := format('%s-01-01', y + 1);
    ELSE
        end_ts := format('%s-%s-01', y, lpad((m + 1)::text, 2, '0'));
    END IF;

    -- 이미 존재 시 skip (V030 사전 생성분 + 직전 cron 생성분)
    IF EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'audit' AND c.relname = pname
    ) THEN
        RETURN 'EXISTS: ' || pname;
    END IF;

    EXECUTE format(
        'CREATE TABLE audit.%I PARTITION OF audit.schedule_audit_log '
        || 'FOR VALUES FROM (%L) TO (%L)',
        pname, start_ts, end_ts);

    -- NFR-SEC-004 방어 — PUBLIC mutation 차단 (트리거가 1차, REVOKE 가 2차).
    EXECUTE format('REVOKE UPDATE, DELETE, TRUNCATE ON audit.%I FROM PUBLIC', pname);

    RETURN 'CREATED: ' || pname;
END;
$$;

COMMENT ON FUNCTION audit.ensure_month_partition(DATE) IS
    'ST-SEC-3 rolling-window — 해당 월 audit partition 미존재 시 생성 (idempotent). PartitionMaintenanceScheduler 가 매월 호출.';
