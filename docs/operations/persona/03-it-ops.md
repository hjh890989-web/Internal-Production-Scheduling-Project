# IT_OPS — IT 운영팀 가이드

**Role**: IT_OPS (Keycloak) | **권한 등급**: P2 (시스템)
**책임**: 마스터·Actuator·Grafana 운영 + 베타 / PROD 배포 + DR + 보안

> IT_OPS 는 시스템의 **인프라 + 보안 + 관측 책임자**. 모든 Sprint 6 NFR 대시 모니터링 +
> Phase 4 의 5 phase 마일스톤 실행 (STG → 베타 → 교육 → DR → PROD cutover).

---

## 1. 접근 가능 화면

| 경로 | 화면 | 용도 |
|---|---|---|
| `/home` ~ `/audit/restore` | 모든 화면 | 모니터링 + 검증 (RBAC IT_OPS 모든 페이지 통과) |
| Grafana | http://stg.scheduling.internal:3000 | 7 대시 (5 base + 2 sprint 6 신규) |
| Prometheus | :9090 | metric raw query (디버깅) |
| Loki | :3100 | 로그 검색 (90일 보존) |
| Keycloak Admin | :8090 | realm + role + 사용자 관리 |
| pgAdmin / DBeaver | postgres:5432 | DB 직접 query (read only 권장) |

---

## 2. Phase 4 운영 체크리스트 (5 phase)

### 2.1 Phase 4-A — STG 부팅 (Week 1)

- [ ] `cp .env.stg.example .env.stg` + vault secret 주입 (6 신규 키)
- [ ] `docker compose --env-file .env.stg up -d`
- [ ] Flyway **V001~V033** 자동 적용 로그 확인 (V033 = PRODUCT_PRIORITY + KD_ORDER, Sprint 7 carry-over)
- [ ] `./scripts/seed-stg-beta-data.sh` 실행 (BS-01~05 sample seed)
- [ ] Grafana 7 대시 표시 확인
- [ ] Keycloak realm `scheduling` import + LDAP/AD sync
- [ ] 베타 사용자 5명 SSO 진입 검증

### 2.2 Phase 4-B — 베타 시나리오 (Week 2)

- [ ] BS-01 ~ BS-05 5 시나리오 × Planner 인수 (`docs/operations/beta-scenarios/`)
- [ ] 각 시나리오 실패 0건 + KPI 영향 확인
- [ ] **🆕 BS-06 후보 활성 (Sprint 7 carry-over)** — DI-07/08 시드 입력 후 진행
  - `SEED_V12V13=1 ./scripts/seed-stg-beta-data.sh` 재실행 (sample 시드 추가)
  - Planner `/vc/capacity-queue` 진입 가능 확인 (Tab1 split + Tab2 supplement)

### 2.3 Phase 4-C — 사용자 교육 (Week 3)

- [ ] 4 페르소나 가이드 배포 (`docs/operations/persona/`)
- [ ] 영상 녹화 (Planner 일일 루틴 ~15분)
- [ ] Q&A 세션 × 4회 (페르소나별)

### 2.4 Phase 4-D — DR + 보안 (Week 4)

- [ ] pg_basebackup 시나리오 — `docs/operations/backup-restore.md`
- [ ] PITR 시점 복원 검증 (어제 14:30 → 복원)
- [ ] Keycloak LDAP/AD sync 검증
- [ ] V026 audit immutability — DBA 권한도 UPDATE/DELETE 거부 확인
- [ ] Alertmanager + Slack 룰 활성 (NS-S04 < 95%)

### 2.5 Phase 4-E — PROD cutover (Week 5)

- [ ] PROD cutover 게이트 11 KPI 모두 통과 (`Phase-4_EntryPlan §7`)
- [ ] 운영팀 인수 회의
- [ ] Phase 5 (PROD) 진입 승인

---

## 3. 일상 모니터링 매트릭스

### 3.1 Grafana 대시 — 매일 확인

| 대시 | panel | 임계값 |
|---|---|---|
| **scheduling-overview** | API p95 (matrix/slots) | < 800ms |
| | Resilience4j Kakao retry | retry 비율 < 5% |
| | Resilience4j circuit state | OPEN 발생 0 |
| | audit_log INSERT rate | mutation 추적 정합 |
| | HikariCP active/idle | active < 18 (pool 20 중 90%) |
| | JVM heap | used < 1.8GB (max 2GB) |
| **business-kpi** | NS-S04 도달률 | ≥ 95% |
| | NS-S09 신규 라인 | ≥ 90% |
| | BR-V07 위반 | 0 (일별) |
| | NS-S07 D-1 준수율 | ≥ 98% |
| | K-V02 가동률 | ≥ 85% |

### 3.2 Loki 로그 — 이상 시 검색

```logql
# 최근 5분 ERROR
{job="scheduling-app"} |= "ERROR" | last 5m

# 특정 사용자 traceId
{userId="planner-001"} | line_format "{{.traceId}} {{.message}}"

# BR-V07 위반 시도
{level="WARN"} |= "BR-V07"
```

---

## 4. 비상 대응

### 4.1 시스템 장애 — 1차 진단

1. Grafana scheduling-overview 대시 → 어떤 panel 이상?
2. Loki — 이상 시점 ±5분 ERROR 검색
3. backend container `docker compose logs --tail=200 backend`
4. DB `SELECT * FROM pg_stat_activity WHERE state != 'idle'`

### 4.2 DB 손상 — PITR

- `docs/operations/backup-restore.md` 참조
- RTO 1시간 / RPO 15분

### 4.3 Keycloak 장애 — IdP 페일오버

- `docs/operations/idp-failover.md` 참조
- local fallback 활성 (DEV/STG 한정)

### 4.4 Resilience4j Kakao CB OPEN

- 30초 자동 half-open
- 지속 OPEN — Kakao 측 webhook 확인 + KAKAO_BOT_TOKEN 갱신

---

## 5. 핵심 API (운영 디버깅용)

| 엔드포인트 | 용도 |
|---|---|
| `/actuator/health` | UP/DOWN + 종속 service 상태 |
| `/actuator/prometheus` | metric raw text |
| `/actuator/loggers/com.scheduling` | 동적 log level 변경 |
| `GET /api/v1/audit/snapshot?table=&rowPk=&at=` | EP-19 forensic |
| `GET /api/v1/audit/timeline?...` | audit history |
| `GET /api/v1/kpi/measurements?from=&to=` | EP-47 KPI raw |
| `POST /api/v1/kpi/measurements/{kpiCode}` | KPI 수동 기록 |
| `POST /api/v1/schedule/vc/capacity-overflow/split` 🆕 | BR-V12 capa 분리 미리보기 (PLANNER 단독, debug 용으로 IT_OPS curl 가능) |
| `POST /api/v1/schedule/vc/capacity-overflow/supplement` 🆕 | BR-V13 KD 잔량 보충 (PLANNER 단독, [BS-06](../beta-scenarios/06-capacity-overflow-kd-supplement.md)) |

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — IT_OPS 페르소나 가이드 + Phase 4 체크리스트 |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over — Flyway V033 명시 + BS-06 후보 (DI-07/08 시드 SEED_V12V13=1) + capacity-overflow REST 2 endpoint 진단 추가 |
