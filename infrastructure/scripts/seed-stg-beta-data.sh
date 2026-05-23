#!/usr/bin/env bash
# =============================================================================
# STG 베타 시드 데이터 — Phase 4-A (Phase-4_EntryPlan §4)
# =============================================================================
# DS-VC-CONSTRAINT-47 + 1주 horizon vc_schedule + business_kpi 9 baseline.
# Flyway 가 V001~V032 적용 후 본 스크립트가 운영 시드 추가.
#
# 사용: ./seed-stg-beta-data.sh
#
# 사전: docker compose --env-file .env.stg up -d postgres redis
#       Flyway migration 완료 확인 (backend container healthy)
# =============================================================================

set -euo pipefail

DB_HOST="${POSTGRES_HOST:-postgres}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-scheduling_stg}"
DB_USER="${POSTGRES_USER:-app_user}"
PSQL="docker compose exec -T -e PGPASSWORD=${POSTGRES_PASSWORD} postgres \
       psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -v ON_ERROR_STOP=1"

echo "==============================================="
echo "Phase 4-A STG 베타 시드 데이터 import 시작"
echo "==============================================="

# ---------- 1. DS-VC-CONSTRAINT-47 마스터 시드 (Sprint 2 datasets) ----------
echo "[1/4] DS-VC-CONSTRAINT-47 마스터 import"
SEED_SQL="backend/app/src/test/resources/datasets/DS-VC-CONSTRAINT-47/master_seed.sql"
if [ ! -f "$SEED_SQL" ]; then
    echo "ERROR: $SEED_SQL 미존재 — git repo root 에서 실행 필요"
    exit 1
fi
$PSQL < "$SEED_SQL"
echo "  → 47 품번 vc_constraint + 5 vc_hose_rule + 4 line_type seed 완료"

# ---------- 2. 1주 horizon (2026-06-01 ~ 2026-06-07) 시드 ----------
echo "[2/4] 1주 horizon vc_schedule baseline (CANDIDATE 상태)"
$PSQL <<'EOF'
-- 베타 운영 1주 horizon — LP-01~04 + IC-01 × slot × rotation
WITH days AS (
    SELECT generate_series('2026-06-01'::date, '2026-06-05'::date, '1 day'::interval)::date AS prod_date
),
machines AS (
    SELECT * FROM (VALUES ('LP-01', 8), ('LP-02', 8), ('LP-03', 8), ('LP-04', 8), ('IC-01', 6)) AS m(machine_id, slots)
),
slot_seq AS (SELECT generate_series(1, 8) AS slot),
rot_seq  AS (SELECT generate_series(1, 18) AS rotation)
INSERT INTO app.vc_schedule (
    vc_schedule_id, hose_id, machine_id, slot_position, production_date, rotation_no,
    angle_id, planned_qty, status, linked_order_ids, created_at, updated_at
)
SELECT
    gen_random_uuid(), '29673-2R060', m.machine_id, s.slot, d.prod_date, r.rotation,
    'ANG-A-S' || s.slot, 100, 'CANDIDATE', '', now(), now()
FROM days d, machines m, slot_seq s, rot_seq r
WHERE s.slot <= m.slots
ON CONFLICT DO NOTHING;
EOF
echo "  → 1주 horizon × 5 머신 × ~7 슬롯 × 18 회전 = ~6,300 row baseline seed"

# ---------- 3. business_kpi.definition baseline KPI 값 ----------
echo "[3/4] business_kpi.measurement baseline (오늘 기준)"
$PSQL <<'EOF'
-- 베타 시작 KPI 측정값 (실 운영 데이터 누적 전 baseline)
INSERT INTO business_kpi.measurement (kpi_code, measured_date, metric_value, threshold, above_target, source)
SELECT d.kpi_code, CURRENT_DATE, 0.0, d.threshold,
       CASE WHEN d.target_dir = 'lower' THEN TRUE ELSE FALSE END,
       'beta-baseline'
FROM business_kpi.definition d
ON CONFLICT (kpi_code, measured_date) DO NOTHING;
EOF
echo "  → 9 KPI baseline 영속 — Grafana business-kpi 대시 즉시 query 가능"

# ---------- 4. 시드 검증 ----------
echo "[4/5] 시드 검증"
$PSQL <<'EOF'
SELECT 'vc_constraint' AS tbl, COUNT(*) AS row_count FROM master.vc_constraint
UNION ALL SELECT 'vc_hose_rule',   COUNT(*) FROM master.vc_hose_rule
UNION ALL SELECT 'line_type',      COUNT(*) FROM master.line_type
UNION ALL SELECT 'vc_schedule',    COUNT(*) FROM app.vc_schedule
UNION ALL SELECT 'kpi_definition', COUNT(*) FROM business_kpi.definition
UNION ALL SELECT 'kpi_baseline',   COUNT(*) FROM business_kpi.measurement WHERE source = 'beta-baseline'
ORDER BY tbl;
EOF

# ---------- 5. (옵션) V033 BR-V12·V13 sample seed — BS-06 활성 조건 ----------
# Sprint 7 v1.1 carry-over — BS-06 (Phase 4-B 후반) 진입 시점에 IT_OPS 가 실행.
# 환경변수 SEED_V12V13=1 인 경우만 적용 (기본 비활성 — BS-01 ~ BS-05 와 분리).
if [ "${SEED_V12V13:-0}" = "1" ]; then
    echo "[5/5] (옵션) V033 BR-V12·V13 sample seed — BS-06 활성 조건"
    $PSQL <<'EOF'
-- DI-07 PRODUCT_PRIORITY — rank ASC + effective_from CURRENT_DATE (즉시 효력)
INSERT INTO master.product_priority
    (hose_id, priority_rank, rationale, effective_from, effective_to, updated_at, updated_by)
VALUES
    ('29673-2R060', 1, 'VIP 고객 X사',    CURRENT_DATE, NULL, now(), 'beta-seed'),
    ('28422-2M800', 2, '긴급 수주',       CURRENT_DATE, NULL, now(), 'beta-seed'),
    ('28421-2M800', 3, '일반',            CURRENT_DATE, NULL, now(), 'beta-seed')
ON CONFLICT (hose_id) DO NOTHING;

-- DI-08 KD_ORDER — 1 sample OPEN row per hose
INSERT INTO master.kd_order
    (kd_order_id, hose_id, order_qty, remaining_qty, order_date, customer_code,
     status, updated_at, updated_by)
VALUES
    (gen_random_uuid(), '29673-2R060', 100, 100, '2026-06-01', 'CUST-X',
     'OPEN', now(), 'beta-seed'),
    (gen_random_uuid(), '28422-2M800',  80,  80, '2026-06-01', 'CUST-Y',
     'OPEN', now(), 'beta-seed')
ON CONFLICT (kd_order_id) DO NOTHING;

SELECT 'product_priority' AS tbl, COUNT(*) AS row_count FROM master.product_priority
UNION ALL SELECT 'kd_order', COUNT(*) FROM master.kd_order WHERE status = 'OPEN'
ORDER BY tbl;
EOF
    echo "  → PRODUCT_PRIORITY 3 + KD_ORDER 2 OPEN seed — BS-06 진입 준비 완료"
else
    echo "[5/5] V033 sample seed 생략 (SEED_V12V13=1 로 재실행 시 활성)"
fi

echo "==============================================="
echo "Phase 4-A STG 베타 시드 import 완료"
echo "  - Planner 가 /vc/simview 진입 → 회전 격자 즉시 표시"
echo "  - Grafana business-kpi 대시 → 0% baseline 표시 (실 운영 누적 시 갱신)"
echo "  - 베타 시나리오 5건 진행 가능 (Phase-4_EntryPlan §5)"
if [ "${SEED_V12V13:-0}" = "1" ]; then
    echo "  - 🆕 BS-06 (BR-V12·V13 capacity-queue) 활성 조건 충족 — Planner /vc/capacity-queue 진입 가능"
fi
echo "==============================================="
