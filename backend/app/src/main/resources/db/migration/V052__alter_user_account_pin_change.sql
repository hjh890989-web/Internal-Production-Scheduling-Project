-- =============================================================================
-- V052__alter_user_account_pin_change.sql — Sprint 22 ST-SEC-2 TK-SEC-2-1
-- =============================================================================
-- NFR-SEC-007 PIN 30일 강제 변경 정책 — last_pin_change_at 컬럼 추가.
--
--   - PIN 마지막 변경 시각. 로그인 시 now - last_pin_change_at > 30일 → pinExpired=true
--     → 강제 변경 화면 redirect (취소 불가).
--   - 신규 row 는 DEFAULT now() (AppUser @CreationTimestamp 와 정합 — INSERT 시 자동).
--   - 기존 row 는 created_at 으로 backfill (가입 시점 = PIN 최초 설정 시점 근사).
--
-- audit: user_account audit 트리거가 jsonb 에 본 컬럼 포함 (pin_hash 만 NFR-SEC-005 제외).
-- =============================================================================

ALTER TABLE app.user_account
    ADD COLUMN IF NOT EXISTS last_pin_change_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 기존 사용자 backfill — 가입 시각 기준 (seed 사용자 포함)
UPDATE app.user_account
    SET last_pin_change_at = created_at
    WHERE last_pin_change_at >= now() - INTERVAL '1 minute';

COMMENT ON COLUMN app.user_account.last_pin_change_at IS
    'NFR-SEC-007 — PIN 마지막 변경 시각. now-30일 초과 시 강제 변경 (ST-SEC-2). reset 시 now-31일 강제 set (ST-SEC-4).';
