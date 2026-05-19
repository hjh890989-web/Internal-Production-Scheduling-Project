# Sprint 0 완료 보고서 (Phase 3 진입 게이트)

**Sprint**: S0 | **기간**: 2026-05-15 ~ 2026-05-19 (5일) | **상태**: ✓ 완료
**작성**: 2026-05-20 | **결재**: 작성 — Claude Code, 검토 대기 — STK-01 + STK-08

---

## 1. 목표 (PLAN-001)

> "Phase 3 진입 게이트 — 인프라·인증·관측·CI/CD·배포·백업·횡단 룰·마스터 검증.
> Sprint 1 핵심 Epic (EP-01·04·30-2) 의 모든 의존성 사전 해소."

---

## 2. 완료 Epic·Story 매트릭스

| Epic | 제목 | Story 완료 | 산출 |
|---|---|---|---|
| **EP-00** | Docker Compose 기반 | ST-00-1·2·3 | 12 서비스 compose + Spring Boot 골격 + React SPA |
| **EP-30** | Keycloak 24 LTS | ST-30-1 | realm-export + IdP placeholder + emergency 3 계정 |
| **EP-31** | 관측성 (Prom+Loki+Grafana) | ST-31-1·2 | Actuator → Prometheus → 4 dashboard + Loki 90일 + AlertManager |
| **EP-32** | CI/CD (Jenkins+Trivy+Sonar+Slack) | ST-32-1·2 | Jenkins LTS + Jenkinsfile.backend (10 stage) + Trivy + SonarQube CE + Slack 60초 |
| **EP-33** | Blue-Green 배포 + 백업 | ST-33-1·2 (TK-33-2-3 STG 드릴은 Sprint 3+) | docker-compose.{stg,prod}.yml + Harbor + pg_basebackup + WAL 5분 archive |
| **EP-34** | KST 시간 통일 | ST-34-3 | Spring TimeZoneConfig + Clock bean + ArchUnit 9 룰 + KstBoundaryTest 7 |
| **EP-99** | 마스터 데이터 검증 | ST-99-1·2 | 5 Python 스크립트 + 5 리포트 + 회귀 SQL — VC 46품번/EX 46품번 위반 0건 |

총 **EP 7개 · Story 13개 · Task 38개 완료** (보고일 기준).

---

## 3. 핵심 지표 (KPI 달성)

| 지표 | 목표 | 실측 | 상태 |
|---|---|---|---|
| Docker 서비스 healthy | 12/12 | 12/12 | ✓ |
| Prometheus scrape targets UP | 4/4 | 4/4 (backend·postgres·keycloak·prometheus) | ✓ |
| Loki 로그 수집 서비스 | ≥ 12 | 13 | ✓ |
| Grafana 대시보드 | 4 (KPI + System + App + Logs) | 4 | ✓ |
| AlertManager alert rules | ≥ 3 | 3 (BackendDown·PostgresDown·KeycloakDown) | ✓ |
| 백업 풀백업 검증 | tar.gz 정상 | 4.2 MB (DEV 빈 DB) | ✓ |
| WAL archive 동작 | 5분 단위 | 강제 switch 시 즉시 archive 확인 | ✓ |
| ArchUnit 모듈 경계 위반 | 0 | 0 (7 모듈) | ✓ |
| 마스터 K/L열 위반 | 0 | 0 (VC 46품번) | ✓ |
| 마스터 B열 위반 | 0 | 0 (EX 46품번) | ✓ |
| BR-V17 영향 품번 식별 | 7 (예상) | 7 (사전 예측 정합) | ✓ |

---

## 4. 주요 commit (시간순)

```
65359e6 feat(security): NFR-SEC-007 v1.5 사번 로그인 + PIN 4자리 + 5회/10분 잠금
6af607b feat(api): TK-01-1-3 Multipart 업로드 엔드포인트 + 비동기 추적 (≤2초)
dad27b7 feat(parser): TK-01-1-2 워크북 헤더 자동 분류기 (4종 SourceType)
ca4476f feat(parser): TK-01-1-1 Apache POI XSSF 스트리밍 reader (10k row ≤256MB)
81d33a5 feat(master): TK-99-2-3 BR-V17 영향 7품번 운영 점검
4537b52 feat(master): TK-99-2-2 규격 분포 통계 + VC/EX 사양 일관성
d7fa173 feat(master): TK-99-2-1 압출 B열(규격) 정수형 무결성 검증
ed8b27e feat(master): TK-99-1-3 성형 마스터 무결성 회귀 SQL 4종
c73500f feat(master): TK-99-1-2 특수 제약 cross-check (BR-V14·V15·V16)
7b566f6 feat(master): TK-99-1-1 성형 K/L열 47품번 무결성 검증
88b5d90 test(time): TK-34-3-2 KST ArchUnit + 경계 일자 단위 테스트 (BR-X04)
3ee13bd feat(time): TK-34-3-1 KST 시간 통일 (BR-X04) — Spring·Frontend
e51b95f feat(slack): TK-32-2-3 Jenkins → Slack 알림 + Trivy/Sonar/Deploy 통합
64be7b4 feat(sonar): TK-32-2-2 SonarQube CE + Quality Gate 'Scheduling'
a83257a feat(security): TK-32-2-1 Trivy 이미지·의존성·IaC 취약점 스캔 게이트
55db39c feat(wal): TK-33-2-2 WAL continuous archiving + 통합 compose
f0807c3 feat(backup): TK-33-2-1 PG 풀백업 + systemd timer + on-demand profile
f47a1e4 feat(alerting): TK-31-2-3 AlertManager + Slack webhook + 통합 부팅
ca0465c feat(grafana): TK-31-2-2 Grafana Loki·Tempo datasource + Logs Overview
89a1d6e feat(loki): TK-31-2-1 Loki + Promtail (90일 retention)
1e05937 feat(grafana): TK-31-1-3 17 KPI 대시보드 골격 + Prometheus datasource
b6494b4 feat(prometheus): TK-31-1-2 Prometheus scrape + 30일 retention
2caf6b4 feat(metrics): TK-31-1-1 Spring Actuator + Micrometer Prometheus
427cbc1 feat(secrets): TK-33-1-3 환경별 변수 분리 + secrets 관리 정책
... (TK-33-1-* + TK-32-1-* + TK-30-1-* + TK-00-* 등 Sprint 0 초기 작업)
```

GitHub Issue 38개 모두 Closed + Project Status = Done.

---

## 5. 운영 결정·예외

| 항목 | 결정 |
|---|---|
| TK-33-2-3 (STG PITR 드릴) | **Sprint 3+ 로 이연** — STG 환경 미구축. Sprint 0 baseline 에서 절차 문서만 작성 (`docs/operations/backup-restore.md`) |
| TK-30-1-2·3 (SAML/OIDC 페더레이션 + Local fallback) | Sprint 1 ST-30-2 (RBAC 활성) 와 묶어 진행 — IdP 사전 통합 대기 |
| NFR-SEC-007 v1.4 → v1.5 (12자/3종 → 사번+PIN) | [DECISION-001](../2.Decisions/DECISION-001_NFR-SEC-007_v1.5_2026-05-19.md) 참조 |
| Keycloak realm 신정책 적용 | 다음 fresh boot 또는 PROD 배포 시 — 사용자 명시 결재 필요 (DB volume 재초기화) |

---

## 6. dual-review 대기 (BR-X05)

ST-99-1·2 산출 6 리포트의 P1 김정훈 주임 + STK-08 IT lead 사인오프 — 사용자 측 결재 작업.

```
tools/master_validation/reports/
  vc_master_kl_2026-05-19.md
  cross_check_special_rules_2026-05-19.md
  ex_master_b_2026-05-19.md
  spec_distribution_2026-05-19.md
  br_v17_impact_2026-05-19.md
  (TK-99-1-3 회귀 SQL 은 코드 리뷰만 — Sprint 1+ Flyway 후 실행)
```

---

## 7. Sprint 1 진입 조건

- [x] 12 Docker 서비스 healthy
- [x] backend bootJar 빌드 OK + 47 tests PASSED
- [x] Trivy + SonarQube 게이트 정의 (실행은 Jenkins job 활성 후)
- [x] CI/CD Jenkinsfile.backend 10 stage 정의
- [x] 마스터 데이터 무결성 베이스라인 확정
- [x] KST 통일 ArchUnit 강제 (production code Instant.now() 0건)
- [ ] dual-review 6 리포트 사인오프 (Sprint 1 진입 후에도 병행 가능)
- [ ] Keycloak realm v1.5 정책 적용 (PROD 배포 시 fresh boot)

→ **Sprint 1 (EP-01 수주통합) 진입 OK**

---

## 8. Sprint Review 데모 가능 항목

1. **Docker 12 서비스 부팅** — `docker compose up -d` → 12 healthy
2. **Grafana 대시보드** — http://127.0.0.1:3000 → 4 dashboards 자동 import (admin/Dev_Grafana_Admin_2026)
3. **Prometheus + AlertManager** — alert rule 3건 inactive 상태 확인
4. **백업 on-demand 실행** — `docker compose --profile backup run --rm pg-backup` → tar.gz 생성
5. **마스터 검증 스크립트** — `python tools/master_validation/validate_vc_master_kl.py` → 위반 0건 리포트
6. **ArchUnit 게이트** — 의도적 `Instant.now()` 추가 후 `./gradlew :app:test` → FAILED 시연

---

## 9. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | Claude Code (검토 대기) | 초안 — Sprint 0 완료 시점 정합 |
