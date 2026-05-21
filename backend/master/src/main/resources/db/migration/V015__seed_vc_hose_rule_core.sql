-- =============================================================================
-- V015__seed_vc_hose_rule_core.sql — TK-21-2-2 (EP-21 ST-21-2)
-- =============================================================================
-- 47품번 중 핵심 운영 룰 보유 3건 (28422-08HA0·28422-2M800·28421-2M800) seed.
-- ON CONFLICT DO UPDATE — 멱등 (재실행 안전).
--
-- ST-99-1 검증 결과 (2026-Q1 v1.4 마스터 분석):
--   28422-08HA0  — LP-01 단일 셋팅, 동시 1슬롯 (BR-V14)
--   28422-2M800  — 우측 셋팅, ≤2 동시 (BR-V16, REQ-FUNC-VC-025)
--   28421-2M800  — 좌측 셋팅, ≤2 동시 (BR-V15, REQ-FUNC-VC-026)
-- =============================================================================

INSERT INTO master.vc_hose_rule
  (hose_id, machine_pin, max_concurrent_slots, side_lock, lp_only, notes, updated_by)
VALUES
  ('28422-08HA0', 'LP-01', 1, NULL,    TRUE,  'BR-V14 LP-01 단일 셋팅 호기',           'seed-v015'),
  ('28422-2M800', NULL,    2, 'RIGHT', FALSE, 'BR-V16 REQ-FUNC-VC-025 우측 셋팅·≤2', 'seed-v015'),
  ('28421-2M800', NULL,    2, 'LEFT',  FALSE, 'BR-V15 REQ-FUNC-VC-026 좌측 셋팅·≤2', 'seed-v015')
ON CONFLICT (hose_id) DO UPDATE SET
    machine_pin          = EXCLUDED.machine_pin,
    max_concurrent_slots = EXCLUDED.max_concurrent_slots,
    side_lock            = EXCLUDED.side_lock,
    lp_only              = EXCLUDED.lp_only,
    notes                = EXCLUDED.notes,
    updated_at           = now(),
    updated_by           = EXCLUDED.updated_by;
