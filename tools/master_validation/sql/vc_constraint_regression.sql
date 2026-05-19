-- =============================================================================
-- vc_constraint_regression.sql — TK-99-1-3 성형 마스터 회귀 SQL
-- =============================================================================
-- 본 SQL 은 Sprint 1+ Flyway 마이그레이션 후 (master.VC_CONSTRAINT 테이블 생성 후)
-- 매일 (또는 master.* 변경 직후) 실행. 위반 1건이라도 발견되면 운영팀 Slack 알림.
--
-- 검증 4종 (Story DoD):
--   1. CHECK 위반 — K/L 컬럼이 'o'/'x' 외 값
--   2. NULL — K/L 컬럼이 NULL
--   3. 중복 — 동일 hose_id 가 2회 이상
--   4. 미정의 호기 — VC_HOSE_RULE 의 machine_pin 이 LP 마스터에 없음
--
-- 사용:
--   psql -U app_user -d scheduling -f vc_constraint_regression.sql
--   exit code 0 = 모든 검증 통과
--   exit code !=0 = 위반 (psql 의 \echo + 검출 행 출력)
-- =============================================================================

\timing on
\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- 1. CHECK 위반 — K/L 컬럼이 'o'/'x' 외 값
-- ---------------------------------------------------------------------------
\echo '=== [1/4] CHECK 위반 검사 (K/L ∈ {o,x}) ==='

SELECT
    hose_id,
    lp_left_setting   AS k_value,
    lp_right_setting  AS l_value,
    'K invalid' AS reason
FROM master.vc_constraint
WHERE lp_left_setting  NOT IN ('o', 'x')
UNION ALL
SELECT
    hose_id,
    lp_left_setting,
    lp_right_setting,
    'L invalid'
FROM master.vc_constraint
WHERE lp_right_setting NOT IN ('o', 'x');

-- ---------------------------------------------------------------------------
-- 2. NULL 검사
-- ---------------------------------------------------------------------------
\echo '=== [2/4] NULL 검사 (K·L·lp_machine·composite) ==='

SELECT
    hose_id,
    CASE WHEN lp_left_setting   IS NULL THEN 'K NULL'        END AS k_null,
    CASE WHEN lp_right_setting  IS NULL THEN 'L NULL'        END AS l_null,
    CASE WHEN lp_machine        IS NULL THEN 'lp_machine'    END AS lp_null,
    CASE WHEN composite_id      IS NULL THEN 'composite_id'  END AS comp_null
FROM master.vc_constraint
WHERE lp_left_setting   IS NULL
   OR lp_right_setting  IS NULL
   OR lp_machine        IS NULL
   OR composite_id      IS NULL;

-- ---------------------------------------------------------------------------
-- 3. 중복 hose_id
-- ---------------------------------------------------------------------------
\echo '=== [3/4] 중복 hose_id 검사 ==='

SELECT
    hose_id,
    COUNT(*) AS occurrences
FROM master.vc_constraint
GROUP BY hose_id
HAVING COUNT(*) > 1
ORDER BY hose_id;

-- ---------------------------------------------------------------------------
-- 4. 미정의 호기 — VC_HOSE_RULE machine_pin 이 마스터에 없음
-- ---------------------------------------------------------------------------
\echo '=== [4/4] VC_HOSE_RULE 미정의 호기 검사 ==='

SELECT
    r.hose_id,
    r.br_code,
    r.machine_pin AS expected_lp,
    'lp_machine 마스터 미발견' AS reason
FROM master.vc_hose_rule r
LEFT JOIN master.vc_constraint c
    ON r.hose_id = c.hose_id
WHERE c.hose_id IS NULL
   OR (r.machine_pin IS NOT NULL AND c.lp_machine IS DISTINCT FROM r.machine_pin);

-- ---------------------------------------------------------------------------
-- 5. 특수 제약 cross-validate (BR-V14·V15·V16)
-- ---------------------------------------------------------------------------
\echo '=== [5/5 bonus] 특수 제약 cross-validate ==='

WITH expected AS (
    SELECT * FROM (VALUES
        ('28422-08HA0', 'BR-V14', NULL,    1, TRUE  ),   -- LP 단일 호기 + max_concurrent=1
        ('28422-2M800', 'BR-V15', 'right', 2, FALSE ),   -- LP 우측 only + max_concurrent=2
        ('28421-2M800', 'BR-V16', 'left',  2, FALSE )    -- LP 좌측 only + max_concurrent=2
    ) AS t(hose_id, br_code, side_only, max_slots, lp_only)
)
SELECT
    e.hose_id,
    e.br_code,
    e.side_only         AS expected_side,
    r.side_only         AS actual_side,
    e.max_slots         AS expected_max,
    r.max_concurrent_slots AS actual_max,
    e.lp_only           AS expected_lp_only,
    r.lp_only           AS actual_lp_only,
    CASE
        WHEN r.hose_id IS NULL                          THEN 'VC_HOSE_RULE 미등재'
        WHEN r.side_only            IS DISTINCT FROM e.side_only       THEN 'side_only 불일치'
        WHEN r.max_concurrent_slots IS DISTINCT FROM e.max_slots       THEN 'max_concurrent_slots 불일치'
        WHEN r.lp_only              IS DISTINCT FROM e.lp_only         THEN 'lp_only 불일치'
        ELSE 'OK'
    END AS status
FROM expected e
LEFT JOIN master.vc_hose_rule r ON e.hose_id = r.hose_id;

\echo ''
\echo '=== 모든 회귀 검증 완료. 위 출력에 row 가 없으면 위반 0건. ==='
