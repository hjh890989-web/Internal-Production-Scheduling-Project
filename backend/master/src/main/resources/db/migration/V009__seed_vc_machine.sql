-- =============================================================================
-- V009__seed_vc_machine.sql — TK-05-1-1 5 가류기 seed (BR-V05)
-- =============================================================================

INSERT INTO master.vc_machine
    (machine_id, machine_type, total_slots, day_rotations, night_rotations, active, updated_by)
VALUES
    ('LP-01', 'LP', 8, 8, 10, true, 'system:seed'),
    ('LP-02', 'LP', 8, 8, 10, true, 'system:seed'),
    ('LP-03', 'LP', 8, 8, 10, true, 'system:seed'),
    ('LP-04', 'LP', 8, 8, 10, true, 'system:seed'),
    ('IC-01', 'IC', 6, 8, 10, true, 'system:seed')
ON CONFLICT (machine_id) DO NOTHING;
