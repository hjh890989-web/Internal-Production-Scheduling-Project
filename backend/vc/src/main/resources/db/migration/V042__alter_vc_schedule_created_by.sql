-- =============================================================================
-- V042__alter_vc_schedule_created_by.sql — Sprint 16 EP-CONFIRM TK-CONFIRM-2-1
-- =============================================================================
-- BR-X05 (Dual-review): 확정 작성자(plannerId) ≠ 작성자(createdBy) 강제.
--   Sprint 16 까지의 vc_schedule 은 created_by 미보유 — Allocator/Listener 가 actor 미식별로
--   row 를 INSERT. 본 마이그레이션이 컬럼만 신설하고 기존 row 는 NULL 유지 (legacy).
--   Sprint 16 이후 신규 row 는 Allocator·Listener·SwapProposal·Override 등 모든 경로에서
--   actor 가 명시되어 INSERT — application code 가 책임 (서비스 가드 + IT 회귀).
--
-- 참고 — confirmed_by (V022) 와 짝. dual-review 핵심 invariant: confirmed_by <> created_by.
-- =============================================================================

ALTER TABLE app.vc_schedule
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(40);

COMMENT ON COLUMN app.vc_schedule.created_by IS
    'Sprint 16 BR-X05 dual-review — INSERT actor 사번. 확정 시 confirmed_by 와 다름 강제';

-- 인덱스 — 추적 조회 용. selective 낮음으로 부분 인덱스 (NOT NULL 만)
CREATE INDEX IF NOT EXISTS idx_vc_schedule_created_by
    ON app.vc_schedule (created_by) WHERE created_by IS NOT NULL;
