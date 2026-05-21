-- V012 — master.holiday (영업일 캘린더 EP-06 ST-06-1)
-- TK-06-1-1 / REQ-FUNC-VC-008 / BR-X07
-- 월~금 영업일 + 본 테이블 등록 휴일 제외.

CREATE TABLE IF NOT EXISTS master.holiday (
    holiday_date   DATE PRIMARY KEY,
    holiday_name   VARCHAR(100) NOT NULL,
    holiday_type   VARCHAR(20)  NOT NULL CHECK (holiday_type IN ('LEGAL','COMPANY','MAINTENANCE')),
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(40)  NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_holiday_year
    ON master.holiday ((EXTRACT(YEAR FROM holiday_date)));

COMMENT ON TABLE master.holiday IS
    'EP-06: 사내 휴일·법정공휴일·정비일 (영업일 계산 제외) — IT_OPS 관리';

-- 2026년 한국 법정공휴일 seed
INSERT INTO master.holiday (holiday_date, holiday_name, holiday_type) VALUES
  (DATE '2026-01-01', '신정',            'LEGAL'),
  (DATE '2026-02-16', '설날 연휴',       'LEGAL'),
  (DATE '2026-02-17', '설날',            'LEGAL'),
  (DATE '2026-02-18', '설날 연휴',       'LEGAL'),
  (DATE '2026-03-01', '삼일절',          'LEGAL'),
  (DATE '2026-05-05', '어린이날',        'LEGAL'),
  (DATE '2026-05-22', '부처님 오신 날',  'LEGAL'),
  (DATE '2026-06-06', '현충일',          'LEGAL'),
  (DATE '2026-08-15', '광복절',          'LEGAL'),
  (DATE '2026-09-24', '추석 연휴',       'LEGAL'),
  (DATE '2026-09-25', '추석',            'LEGAL'),
  (DATE '2026-09-26', '추석 연휴',       'LEGAL'),
  (DATE '2026-10-03', '개천절',          'LEGAL'),
  (DATE '2026-10-09', '한글날',          'LEGAL'),
  (DATE '2026-12-25', '성탄절',          'LEGAL')
ON CONFLICT (holiday_date) DO NOTHING;
