-- =============================================================================
-- V036 — Sprint 10 EP-AUTH (사번+PIN 인증 활성, NFR-SEC-007)
-- =============================================================================
-- 사번 8자리 숫자 + PIN 4자리 BCrypt (strength 12) + 5회 실패 잠금 + 10분 자동 해제.
-- 4 역할 — PLANNER (생산계획 작성·확정), STK_USER (현장 시뮬뷰·제안),
-- IT_OPS (마스터·관측), READ_ONLY (감사·임원 조회). v1.5 SRS REQ-NF-SEC-007 (2026-05-19 운영 결정).
--
-- 활성 후 효과 — SecurityConfig DEV fallback 제거 가능 + audit_log.actor 정확화 (사번).
-- BR-X02 audit trigger 동봉 — Sprint 9 V035 hotfix 교훈 (schema 신설 시 trigger 누락 방지).
-- NFR-SEC-005 정합 — pin_hash 컬럼은 audit jsonb 에서 제외 (BCrypt 노출 차단).
-- =============================================================================

CREATE TABLE app.user_account (
    employee_id      VARCHAR(8)   PRIMARY KEY
                                  CHECK (employee_id ~ '^[0-9]{8}$'),     -- 숫자 8자리 (regex enforce)
    pin_hash         VARCHAR(60)  NOT NULL,                               -- BCrypt strength 12 (고정 60 char)
    role             VARCHAR(20)  NOT NULL
                                  CHECK (role IN ('PLANNER', 'STK_USER', 'IT_OPS', 'READ_ONLY')),
    failed_attempts  SMALLINT     NOT NULL DEFAULT 0
                                  CHECK (failed_attempts >= 0),
    locked_until     TIMESTAMPTZ,                                          -- 5회 실패 시 now() + 10min
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE app.user_account IS
    'Sprint 10 EP-AUTH NFR-SEC-007 — 사번 8자리 + PIN 4자리 BCrypt + 4 역할';
COMMENT ON COLUMN app.user_account.employee_id IS
    '사번 (숫자 8자리, 영림원 ERP 사번 체계 정합)';
COMMENT ON COLUMN app.user_account.pin_hash IS
    'BCrypt strength 12 hash (60 char). audit jsonb 에서 제외 (NFR-SEC-005)';
COMMENT ON COLUMN app.user_account.role IS
    'PLANNER (작성·확정) / STK_USER (시뮬뷰·제안) / IT_OPS (마스터·관측) / READ_ONLY (조회)';
COMMENT ON COLUMN app.user_account.locked_until IS
    '5회 실패 시 now() + 10min — UserDetailsService 가 LockedException 발생';

-- =============================================================================
-- updated_at 자동 갱신 trigger
-- =============================================================================
CREATE OR REPLACE FUNCTION app.fn_user_account_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_account_updated_at ON app.user_account;
CREATE TRIGGER trg_user_account_updated_at
    BEFORE UPDATE ON app.user_account
    FOR EACH ROW EXECUTE FUNCTION app.fn_user_account_updated_at();

-- =============================================================================
-- BR-X02 audit trigger (V025 패턴 + V035 교훈)
-- pin_hash 는 jsonb 에서 제외 (NFR-SEC-005 — BCrypt 노출 차단, PIN 4자리 brute force 표면 ↓)
-- =============================================================================
CREATE OR REPLACE FUNCTION audit.fn_audit_user_account()
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
        'user_account',
        COALESCE(NEW.employee_id, OLD.employee_id),
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN (row_to_json(OLD)::jsonb - 'pin_hash') END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN (row_to_json(NEW)::jsonb - 'pin_hash') END,
        v_actor, v_reason
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_user_account ON app.user_account;
CREATE TRIGGER trg_audit_user_account
    AFTER INSERT OR UPDATE OR DELETE ON app.user_account
    FOR EACH ROW EXECUTE FUNCTION audit.fn_audit_user_account();
