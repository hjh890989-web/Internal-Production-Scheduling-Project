---
name: flyway-migration-design
description: Flyway 마이그레이션 설계 (Phase 2 SAD §6.1.1 3 schema + PDD v1.7 ADR-010 + baseline + repeatable).
---

# Flyway Migration Design

본 프로젝트 (PostgreSQL 16 + Flyway 10) 마이그레이션 표준. **Phase 2 SAD §6.1.1 + PDD v1.7 ADR-010 — 데이터 의미 기반 3 schema** (Modulith 모듈 ≠ DB schema).

## When to use
- 새 테이블·컬럼 추가 시
- 데이터 백필 (backfill)
- DB 함수·트리거·뷰 변경
- 새 mater 테이블 추가 (BR-V12·13·14 등)

## 3 Schema 분리 (SAD §6.1.1 / PDD v1.7 ADR-010)

| Schema | 용도 | 접근 권한 | 비고 |
|---|---|---|---|
| `app` | 운영 데이터 (PRODUCT·ORDER·VC_SCHEDULE·EX_SCHEDULE 등) | `app_user` read·write | default-schema |
| `audit` | 감사 로그 (ORDER_CHANGE·VC_AUDIT·EX_AUDIT) | `app_user` INSERT only · `auditor` SELECT only · UPDATE·DELETE `REVOKE` | BR-X02 · REQ-NF-SEC-004 (≥3년 보존) |
| `master` | 마스터 데이터 (VC_CONSTRAINT·EX_CONSTRAINT·VC_HOSE_RULE·PRODUCT_PRIORITY·KD_ORDER) | `app_user` SELECT only · `master_admin` UPDATE | BR-X05 dual-review 후 master_admin 적용 |

**Cross-schema FK 허용** — 데이터 의미 결합 (예: `master.VC_HOSE_RULE.hose_id → app.PRODUCT.hose_id`). 권한만 분리.

**Modulith 패키지 ≠ DB schema** — 5 모듈 (`scheduling`·`audit`·`auth`·`mes`·`report`) 패키지 boundary 는 코드 의존만 강제. 모든 모듈은 같은 3 schema 안의 테이블을 자유롭게 read·write (권한 한도 내).

## Baseline (Sprint 0 TK-00-1-1)

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 20260516_000
    baseline-description: 'Initial baseline (Sprint 0 — 3 schema + 3 role)'
    locations:
      - classpath:db/migration/baseline
      - classpath:db/migration/app
      - classpath:db/migration/audit
      - classpath:db/migration/master
    schemas: app,audit,master
    default-schema: app
```

### Baseline SQL (`db/migration/baseline/V20260516_000__schemas_and_roles.sql`)

```sql
-- SAD §6.1.1 / PDD v1.7 ADR-010 — 3 schema + 3 role + KST timezone
CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS master;

CREATE ROLE IF NOT EXISTS auditor      NOLOGIN;
CREATE ROLE IF NOT EXISTS master_admin NOLOGIN;

COMMENT ON SCHEMA app    IS '운영 데이터 (PRODUCT·ORDER·VC_SCHEDULE·EX_SCHEDULE)';
COMMENT ON SCHEMA audit  IS '감사 로그 — INSERT only, ≥3년 보존 (REQ-NF-SEC-004)';
COMMENT ON SCHEMA master IS '마스터 데이터 (제약·우선순위·KD_ORDER)';

-- 권한 부여
GRANT USAGE ON SCHEMA app, master, audit TO app_user;
GRANT ALL    ON ALL TABLES IN SCHEMA app    TO app_user;
GRANT SELECT ON ALL TABLES IN SCHEMA master TO app_user;
GRANT INSERT ON ALL TABLES IN SCHEMA audit  TO app_user;

GRANT USAGE  ON SCHEMA audit TO auditor;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO auditor;

GRANT ALL    ON ALL TABLES IN SCHEMA master TO master_admin;

-- KST timezone (BR-X04)
ALTER DATABASE scheduling SET timezone TO 'Asia/Seoul';
```

## Versioned Migration 표준

### 파일명 규칙
`V<version>__<snake_case_description>.sql`
- `V20260516_001__create_product_table.sql` (app schema)
- `V20260516_002__create_vc_constraint_table.sql` (master schema)
- `V20260516_003__create_vc_audit_table.sql` (audit schema)

버전 — `YYYYMMDD_NNN` (날짜 + 일별 순번). 동일 sprint 충돌 회피.

### 표준 헤더 (운영 테이블)
```sql
-- V20260516_001__create_vc_schedule_table.sql
-- Schema: app
-- Module: scheduling (코드 패키지)
-- BR: BR-V07 (당일 락), BR-X01 (확정 게이트)
-- Story: ST-05-1

SET search_path TO app;

CREATE TABLE vc_schedule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hose_id         VARCHAR(32) NOT NULL REFERENCES app.product(hose_id),
    production_date DATE NOT NULL,
    machine_code    VARCHAR(20) NOT NULL,
    rotation_no     SMALLINT NOT NULL CHECK (rotation_no BETWEEN 1 AND 18),
    composite_grp   SMALLINT CHECK (composite_grp IN (1,2,3,6)),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50) NOT NULL,
    confirmed_at    TIMESTAMP WITH TIME ZONE,
    confirmed_by    VARCHAR(50)
);

CREATE INDEX idx_vc_schedule_production_date ON vc_schedule(production_date);
CREATE INDEX idx_vc_schedule_machine_date ON vc_schedule(machine_code, production_date);

COMMENT ON COLUMN vc_schedule.composite_grp IS 'BR-V07 합금형 그룹 (1·2·3·6)';
COMMENT ON COLUMN vc_schedule.status IS 'DRAFT / CONFIRMED / EXECUTED / CANCELLED';
```

### 표준 헤더 (audit 테이블)
```sql
-- V20260516_010__create_vc_audit_table.sql
-- Schema: audit
-- BR: BR-X02 (mutation audit 강제), REQ-NF-SEC-004 (≥3년 보존)
-- Story: ST-11-2

SET search_path TO audit;

CREATE TABLE vc_audit (
    id           BIGSERIAL PRIMARY KEY,
    schedule_id  UUID NOT NULL,
    action       VARCHAR(20) NOT NULL,  -- CREATE / UPDATE / CONFIRM / DELETE
    actor        VARCHAR(64) NOT NULL,
    actor_role   VARCHAR(20) NOT NULL,  -- ROLE_PLANNER · ROLE_STK_USER · ...
    payload_jsonb JSONB,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vc_audit_schedule ON vc_audit(schedule_id, occurred_at DESC);
CREATE INDEX idx_vc_audit_actor ON vc_audit(actor, occurred_at DESC);

-- 불변성 강제 (REQ-NF-SEC-004)
REVOKE UPDATE, DELETE ON audit.vc_audit FROM PUBLIC;
REVOKE UPDATE, DELETE ON audit.vc_audit FROM app_user;
```

## Repeatable Migration (`R__*.sql`)

뷰·함수·시드 데이터 — 매 배포 마다 재실행. checksum 변경 시만 적용.

```sql
-- R__schedule_views.sql
CREATE OR REPLACE VIEW app.v_schedule_kpi AS
SELECT
    production_date,
    machine_code,
    COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_count,
    SUM(yield_unit) AS total_yield
FROM app.vc_schedule
GROUP BY production_date, machine_code;
```

## 데이터 백필 (backfill)

- 작은 테이블 — 동기 (`UPDATE ... WHERE ...`)
- 큰 테이블 (> 100K row) — chunked (`WHERE id BETWEEN ? AND ?`) + 별도 마이그레이션
- NOT NULL 컬럼 추가 시 — 3 단계 (① nullable 컬럼 추가 → ② backfill → ③ NOT NULL 제약)

```sql
-- V20260520_001__add_yield_nullable.sql (Step 1)
ALTER TABLE app.vc_schedule ADD COLUMN yield_v2 INTEGER;

-- V20260520_002__backfill_yield.sql (Step 2)
UPDATE app.vc_schedule SET yield_v2 = yield_unit WHERE yield_v2 IS NULL;

-- V20260520_003__yield_not_null.sql (Step 3)
ALTER TABLE app.vc_schedule ALTER COLUMN yield_v2 SET NOT NULL;
```

## 검증

- `mvn flyway:info` 또는 `./gradlew flywayInfo` — 적용 상태
- Testcontainers 에서 매 테스트 실행 시 fresh DB 에 자동 적용
- CI — `flyway:validate` 강제 (checksum 변경 차단)

## Rollback 전략

Flyway 는 자동 rollback 없음. 본 프로젝트는 **forward migration only**:
- 실수 시 — 새 마이그레이션 (`V20260517_001__revert_*.sql`) 추가
- PROD 사고 — pg_basebackup + PITR (Phase 2 EP-33)

## Anti-patterns
- 이미 적용된 마이그레이션 파일 수정 (checksum 깨짐)
- `V*` 파일에 `IF NOT EXISTS` (idempotent 가장 — 의도 파악 불가)
- Modulith 모듈 별 schema 생성 (잘못된 가정 — SAD §6.1.1 정본은 의미 기반 3 schema)
- Test 와 PROD migration 파일 분리 (drift)
- Migration 으로 BR 룰 데이터 변경 (코드 + audit 우회)
- audit 테이블에 UPDATE·DELETE 권한 부여 (REQ-NF-SEC-004 위반)

## 참고
- Phase 2 SAD §6.1.1 (3 schema 정본 — `app·audit·master`)
- Phase 2 PDD v1.7 ADR-010 (Schema 의미 기반 분리 PDD 정형화)
- Phase 2 EP-00 ST-00-1 TK-00-1-1 (DB 셋업 baseline)
- Phase 2 EP-33 (백업·복원 PITR) — `Phase 2/4.Tasks/Tasks/EP-33/`
