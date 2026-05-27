-- =============================================================================
-- V040 — Sprint 15 EP-EX-FULL ST-EX-2 sample ex_schedule_candidate seed (DEV/베타)
-- =============================================================================
-- 압출 매트릭스 페이지 (/extrusion-matrix) 가 빈 grid 만 표시 → V040 시드로 V039 sample
-- VcSchedule 과 chain 맞춘 ex candidate row 표시. PLANNER/STK 시각 검증.
--
-- 시드 전략:
-- 1. V039 의 99999-SAMPLE-* vc_schedule row 를 SELECT (sub-select)
-- 2. 각 row 당 ex_candidate 1건 생성 (BR-E01 — extrusion_deadline = vc_production_date - 1)
-- 3. WHERE NOT EXISTS — vc_row_id 기준 idempotent
--
-- vcYield 2531 — BR-E05 reference (29673-2R060 표준). sample 은 100~200 (단순 sample).
-- =============================================================================

INSERT INTO app.ex_schedule_candidate (
    ex_candidate_id, schedule_id, hose_id, vc_row_id,
    vc_production_date, extrusion_deadline, vc_yield, status,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    gen_random_uuid(),       -- schedule_id (batch) — sample 별 별도 batch
    vs.hose_id,
    vs.vc_schedule_id,       -- FK to vc_schedule (V039 sample row)
    vs.production_date,
    vs.production_date - INTERVAL '1 day',   -- BR-E01: D-1 역산
    vs.planned_qty,           -- sample yield = planned_qty (단순)
    'PENDING',
    NOW(), NOW()
FROM app.vc_schedule vs
WHERE vs.hose_id LIKE '99999-SAMPLE-%'
  AND NOT EXISTS (
      SELECT 1 FROM app.ex_schedule_candidate exc
      WHERE exc.vc_row_id = vs.vc_schedule_id
  );
