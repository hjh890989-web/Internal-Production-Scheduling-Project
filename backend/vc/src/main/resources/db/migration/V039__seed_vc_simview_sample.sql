-- =============================================================================
-- V039 — Sprint 14 EP-VC-FULL ST-VC-2 sample VcSchedule seed (DEV/베타)
-- =============================================================================
-- 시뮬뷰 페이지 (/vc/simview) 가 빈 grid 만 표시 (Phase 5+ Order commit → vc 자동 INSERT
-- 흐름 전까지) — 본 시드로 1주 horizon sample 15 row 표시 → PLANNER/STK 시각 검증.
--
-- 시드 namespace — 99999-SAMPLE-* hose_id (실 운영 47 품번과 분리). PROD 진입 시
-- PLANNER 가 본 row 삭제 1회 권장.
--
-- WHERE NOT EXISTS — vc_schedule UNIQUE 가 DEFERRABLE 이라 ON CONFLICT 호환 안 됨
-- (Sprint 8 V028 교훈). 본 migration 재실행 안전 (idempotent).
--
-- vc_machine FK — Testcontainers/DEV 환경 vc_machine 미시드 가능성 → LP-01~04 + IC 시드 동봉.
-- =============================================================================

-- 1. vc_machine 시드 (FK 충족) — PROD 진입 시 이미 존재해서 skip
INSERT INTO master.vc_machine (
    machine_id, machine_type, total_slots, day_rotations, night_rotations,
    active, updated_at, updated_by
)
SELECT * FROM (VALUES
    ('LP-01', 'LP', 8::smallint, 8::smallint, 10::smallint, true, NOW(), 'V039-seed'),
    ('LP-02', 'LP', 8::smallint, 8::smallint, 10::smallint, true, NOW(), 'V039-seed'),
    ('LP-03', 'LP', 8::smallint, 8::smallint, 10::smallint, true, NOW(), 'V039-seed'),
    ('LP-04', 'LP', 8::smallint, 8::smallint, 10::smallint, true, NOW(), 'V039-seed'),
    ('IC',    'IC', 6::smallint, 8::smallint, 10::smallint, true, NOW(), 'V039-seed')
) AS new_machines (machine_id, machine_type, total_slots, day_rotations, night_rotations,
                    active, updated_at, updated_by)
WHERE NOT EXISTS (
    SELECT 1 FROM master.vc_machine m WHERE m.machine_id = new_machines.machine_id
);

-- 2. vc_schedule sample 15 row 시드
INSERT INTO app.vc_schedule (
    vc_schedule_id, hose_id, machine_id, slot_position, production_date,
    rotation_no, angle_id, planned_qty, status, override_reason, created_at, updated_at
)
SELECT * FROM (VALUES
    (gen_random_uuid(), '99999-SAMPLE-A1', 'LP-01', 1::smallint, CURRENT_DATE,     1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-A2', 'LP-01', 2::smallint, CURRENT_DATE,     1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-A3', 'LP-01', 1::smallint, CURRENT_DATE + 1, 1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-A4', 'LP-01', 2::smallint, CURRENT_DATE + 1, 1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-A5', 'LP-01', 1::smallint, CURRENT_DATE + 2, 1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-A6', 'LP-01', 2::smallint, CURRENT_DATE + 2, 1::smallint, 'ANG-A', 100, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B1', 'LP-02', 1::smallint, CURRENT_DATE,     1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B2', 'LP-02', 3::smallint, CURRENT_DATE,     1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B3', 'LP-02', 1::smallint, CURRENT_DATE + 1, 1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B4', 'LP-02', 3::smallint, CURRENT_DATE + 1, 1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B5', 'LP-02', 1::smallint, CURRENT_DATE + 2, 1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-B6', 'LP-02', 3::smallint, CURRENT_DATE + 2, 1::smallint, 'ANG-B', 200, 'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-C1', 'IC',    1::smallint, CURRENT_DATE,     1::smallint, 'ANG-C', 50,  'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-C2', 'IC',    1::smallint, CURRENT_DATE + 1, 1::smallint, 'ANG-C', 50,  'CANDIDATE', '', NOW(), NOW()),
    (gen_random_uuid(), '99999-SAMPLE-C3', 'IC',    1::smallint, CURRENT_DATE + 2, 1::smallint, 'ANG-C', 50,  'CANDIDATE', '', NOW(), NOW())
) AS new_rows (vc_schedule_id, hose_id, machine_id, slot_position, production_date,
               rotation_no, angle_id, planned_qty, status, override_reason, created_at, updated_at)
WHERE NOT EXISTS (
    SELECT 1 FROM app.vc_schedule existing
    WHERE existing.machine_id = new_rows.machine_id
      AND existing.slot_position = new_rows.slot_position
      AND existing.production_date = new_rows.production_date
      AND existing.rotation_no = new_rows.rotation_no
);
