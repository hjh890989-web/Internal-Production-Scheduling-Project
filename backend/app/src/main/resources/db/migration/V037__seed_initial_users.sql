-- =============================================================================
-- V037 — Sprint 10 EP-AUTH 베타 초기 사용자 시드 (NFR-SEC-007 ST-AUTH-7)
-- =============================================================================
-- 베타 사용자 8명 (사번 emp00000001~8) — PLANNER 3 + STK_USER 3 + IT_OPS 1 + READ_ONLY 1.
-- 초기 PIN — 사번 마지막 4자리 ('0001'~'0008'). 베타 운영자가 첫 로그인 후 변경 권장
-- (Sprint 12 EP-MASTER-UI 의 사용자 관리 페이지에서 IT_OPS 가 PIN 재설정 가능).
--
-- pgcrypto crypt(..., gen_salt('bf', 12)) — Spring BCryptPasswordEncoder strength 12 호환.
-- ON CONFLICT DO NOTHING — idempotent (재실행 시 기존 row 보존).
--
-- 베타 PIN 발급표 — docs/operations/initial-users-table.md
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO app.user_account (employee_id, pin_hash, role) VALUES
    ('00000001', crypt('0001', gen_salt('bf', 12)), 'PLANNER'),
    ('00000002', crypt('0002', gen_salt('bf', 12)), 'PLANNER'),
    ('00000003', crypt('0003', gen_salt('bf', 12)), 'PLANNER'),
    ('00000004', crypt('0004', gen_salt('bf', 12)), 'STK_USER'),
    ('00000005', crypt('0005', gen_salt('bf', 12)), 'STK_USER'),
    ('00000006', crypt('0006', gen_salt('bf', 12)), 'STK_USER'),
    ('00000007', crypt('0007', gen_salt('bf', 12)), 'IT_OPS'),
    ('00000008', crypt('0008', gen_salt('bf', 12)), 'READ_ONLY')
ON CONFLICT (employee_id) DO NOTHING;
