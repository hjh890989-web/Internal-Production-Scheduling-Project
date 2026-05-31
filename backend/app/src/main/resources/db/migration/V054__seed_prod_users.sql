-- =============================================================================
-- V054 — Sprint 25 ST-PROD-1 프로덕션 사용자 시드 30명
-- =============================================================================
-- PLANNER 10명   : 00000009~00000018 (베타 00000001~3 PLANNER 유지, 10명으로 확장)
-- STK_USER 15명  : 00000019~00000033 (베타 00000004~6 STK_USER 유지, 15명으로 확장)
-- IT_OPS 3명     : 00000007 유지(V037), 신규 00000034~00000035
-- READ_ONLY 5명  : 00000008 유지(V037), 신규 00000036~00000039
--
-- 초기 PIN — 사번 마지막 4자리 (00000009 → '0009', ..., 00000039 → '0039').
-- 베타 시드 V037 (00000001~8) 과 중복 없음 — ON CONFLICT (employee_id) DO NOTHING
-- 으로 완전 idempotent 보장.
--
-- pgcrypto crypt(..., gen_salt('bf', 12)) — Spring BCryptPasswordEncoder strength 12 호환.
-- last_pin_change_at DEFAULT now() — V052 ALTER 정책 자동 적용 (INSERT 시 트리거 없음,
-- DEFAULT 값 사용).
--
-- Keycloak realm 실 활성화는 Phase 5+ (사내 IT Pre-Phase 의존).
-- 본 시드는 local DB DaoAuth fallback 진입 시점에 적용.
-- 실 사번/role 명단 사내 IT 미확정 — placeholder. 확정 시 V055 갱신 또는 docs 만 갱신.
--
-- PIN 발급표 — docs/operations/prod-users-table_v1.0.md
-- BR-X02 — 첫 로그인 후 PIN 변경 권장 (NFR-SEC-007 v1.5 30일 강제 변경 정책 적용 중)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----- PLANNER 10명 (신규 — 베타 00000001~3 유지) -----
INSERT INTO app.user_account (employee_id, pin_hash, role) VALUES
    ('00000009',  crypt('0009',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000010',  crypt('0010',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000011',  crypt('0011',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000012',  crypt('0012',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000013',  crypt('0013',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000014',  crypt('0014',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000015',  crypt('0015',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000016',  crypt('0016',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000017',  crypt('0017',  gen_salt('bf', 12)), 'PLANNER'),
    ('00000018',  crypt('0018',  gen_salt('bf', 12)), 'PLANNER')
ON CONFLICT (employee_id) DO NOTHING;

-- ----- STK_USER 15명 (신규 — 베타 00000004~6 유지) -----
INSERT INTO app.user_account (employee_id, pin_hash, role) VALUES
    ('00000019',  crypt('0019',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000020',  crypt('0020',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000021',  crypt('0021',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000022',  crypt('0022',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000023',  crypt('0023',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000024',  crypt('0024',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000025',  crypt('0025',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000026',  crypt('0026',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000027',  crypt('0027',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000028',  crypt('0028',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000029',  crypt('0029',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000030',  crypt('0030',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000031',  crypt('0031',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000032',  crypt('0032',  gen_salt('bf', 12)), 'STK_USER'),
    ('00000033',  crypt('0033',  gen_salt('bf', 12)), 'STK_USER')
ON CONFLICT (employee_id) DO NOTHING;

-- ----- IT_OPS 신규 2명 (00000007 은 V037 유지) -----
INSERT INTO app.user_account (employee_id, pin_hash, role) VALUES
    ('00000034',  crypt('0034',  gen_salt('bf', 12)), 'IT_OPS'),
    ('00000035',  crypt('0035',  gen_salt('bf', 12)), 'IT_OPS')
ON CONFLICT (employee_id) DO NOTHING;

-- ----- READ_ONLY 신규 4명 (00000008 은 V037 유지) -----
INSERT INTO app.user_account (employee_id, pin_hash, role) VALUES
    ('00000036',  crypt('0036',  gen_salt('bf', 12)), 'READ_ONLY'),
    ('00000037',  crypt('0037',  gen_salt('bf', 12)), 'READ_ONLY'),
    ('00000038',  crypt('0038',  gen_salt('bf', 12)), 'READ_ONLY'),
    ('00000039',  crypt('0039',  gen_salt('bf', 12)), 'READ_ONLY')
ON CONFLICT (employee_id) DO NOTHING;
