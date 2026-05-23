-- =============================================================================
-- V032__create_business_kpi_measurement.sql — TK-47-1+2+3+4 (EP-47, KPI-001~019)
-- =============================================================================
-- 사업 KPI 일별 시계열 영속 — NS-S01~S09 (보조) + K-V01~06 (성형) + K-E01~06 (압출).
-- Grafana 대시 (business-kpi.json) 직접 query 대상 + Slack 임계값 미달 알림 trigger.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS business_kpi;
COMMENT ON SCHEMA business_kpi IS
    'EP-47 사업 KPI 영속 (KPI-001~019, Sprint 6 EP-47)';

CREATE TABLE IF NOT EXISTS business_kpi.measurement (
    kpi_code       VARCHAR(20)  NOT NULL,          -- 'NS-S04' / 'K-V02' / 'KPI-007' 등
    measured_date  DATE         NOT NULL,
    metric_value   NUMERIC(12,4) NOT NULL,         -- 0.0~100.0 (%) 또는 절댓값
    threshold      NUMERIC(12,4),                  -- 목표 (NULL = 단순 추적)
    above_target   BOOLEAN,                        -- metric_value >= threshold (Slack alert 입력)
    captured_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    source         VARCHAR(40)  NOT NULL DEFAULT 'scheduled',  -- 'scheduled' | 'manual' | 'replay'
    PRIMARY KEY (kpi_code, measured_date)
);

CREATE INDEX IF NOT EXISTS idx_kpi_measurement_date
    ON business_kpi.measurement (measured_date DESC);
CREATE INDEX IF NOT EXISTS idx_kpi_measurement_below
    ON business_kpi.measurement (kpi_code, measured_date) WHERE above_target = FALSE;

COMMENT ON TABLE business_kpi.measurement IS
    'EP-47 KPI 일별 측정값 (REQ-NF-KPI-001~019)';
COMMENT ON COLUMN business_kpi.measurement.above_target IS
    'metric_value >= threshold (Slack alert 입력 — NS-S04 < 95% 등)';

-- 19 KPI 메타 정의 (lookup) — Sprint 6 baseline, 임계값 SRS v1.5 정합
CREATE TABLE IF NOT EXISTS business_kpi.definition (
    kpi_code       VARCHAR(20)  PRIMARY KEY,
    category       VARCHAR(20)  NOT NULL CHECK (category IN ('NS','K-V','K-E')),
    description    TEXT         NOT NULL,
    threshold      NUMERIC(12,4),
    unit           VARCHAR(20)  NOT NULL DEFAULT 'percent',     -- 'percent' | 'count' | 'minutes'
    target_dir     VARCHAR(10)  NOT NULL DEFAULT 'higher'       -- 'higher' = 높을수록 좋음, 'lower' = 낮을수록
        CHECK (target_dir IN ('higher', 'lower'))
);

INSERT INTO business_kpi.definition (kpi_code, category, description, threshold, unit, target_dir) VALUES
  ('NS-S01', 'NS',  'P1·P4 만족도',                95.00, 'percent', 'higher'),
  ('NS-S04', 'NS',  'Kakao 도달률',                95.00, 'percent', 'higher'),
  ('NS-S07', 'NS',  'D-1 압출 deadline 준수율',   98.00, 'percent', 'higher'),
  ('NS-S09', 'NS',  '신규 라인 사용률 (BR-E08)',  90.00, 'percent', 'higher'),
  ('K-V01',  'K-V', '슬롯 점유율',                  85.00, 'percent', 'higher'),
  ('K-V02',  'K-V', '가류기 가동률',               85.00, 'percent', 'higher'),
  ('K-V04',  'K-V', '일중 락 위반 (BR-V07)',        0.00, 'count',   'lower'),
  ('K-E02',  'K-E', '압출 셋업 시간 (BR-E06)',     30.00, 'minutes', 'lower'),
  ('K-E03',  'K-E', 'shift 가동 효율 (BR-E04)',    75.00, 'percent', 'higher')
ON CONFLICT (kpi_code) DO UPDATE SET
    description = EXCLUDED.description,
    threshold   = EXCLUDED.threshold,
    unit        = EXCLUDED.unit,
    target_dir  = EXCLUDED.target_dir;

COMMENT ON TABLE business_kpi.definition IS
    'EP-47 19 KPI 메타 + 임계값 (SRS v1.5 KPI-001~019 정합)';
