# Sprint 25 진입 계획 — EP-PROD-LAUNCH (본격 운영 진입) v1.1

**작성일**: 2026-06-01 | **버전**: 1.1 | **상태**: Phase 4 마지막 sprint **조기 진입 모드 (S25-A / S25-B 분할)**

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S25](PHASE-4_STABILIZATION_v1.0.md) + [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md) + [PLAN-SPRINT-25 v1.0](PLAN-SPRINT-25_EP-PROD-LAUNCH_v1.0.md) + [PLAN-SPRINT-24 v1.1](PLAN-SPRINT-24_EP-OPS-FEEDBACK_v1.1.md) 분할 패턴

---

## 0. v1.0 → v1.1 변경 요약

1. **Pre-Phase 의존 18% 충족 → 정식 진입 불가, 분할.** 베타 ~5일/28일 (운영 누적 18%) + 사용자 발급 0/8 + BETA_REPORT v0.1 골격만 (Go/No-Go 데이터 0%) + 사내 IT Keycloak realm 활성 미확정 + 30명 실 명단 미확정. 정식 v1.0 4 SP 일괄 진입 보류, **data-free 작업만 S25-A 로 분리**, 잔여는 **S25-B carry-over** (Phase 5+ 베타 28일 마감 + Go/No-Go 후 진입). Sprint 24 v1.1 (`fafa2db`) 분할 패턴 재사용.
2. **S25-A 조기 (~1.5 SP, 0.7 PD, 1 Day)** — data-free 작업 일괄:
   - **ST-PROD-2** k6 script 신규 — `infrastructure/k6/load-test-prod.js` (30 VU × 5분 × 4 시나리오: login + 시뮬뷰 조회 + Diff GET + 확정), threshold `p95<800ms` + `error<0.1%` (REQ-NF-PER-004). **실 실행 STG 의존 deferred**.
   - **ST-PROD-3** Blue/Green 정합성 fix 3건 — `deploy.sh` NGINX 경로 `prod-active.conf` 정합 + `BACKEND_IMAGE` → `TAG_BLUE`/`TAG_GREEN` 환경변수 분리, `docker-compose.prod.yml` `depends_on` `backend-green` healthcheck 추가, `DRY_RUN=1` 분기 추가, `rollback.sh` readiness smoke test 1회 검증. **실 deploy 차기**.
   - **ST-PROD-1** Keycloak `V05x__seed_prod_users.sql` 신규 — 30명 (PLANNER 10 + STK 15 + IT_OPS 3 + READ 5), V037 패턴 idempotent `ON CONFLICT`, BCrypt strength 12 + `docs/cutover/PROD_USERS_TABLE_v1.0.md` 명단 표 (사번/role/email placeholder). **실 발급 Keycloak realm 활성 의존 deferred**.
   - **ST-PROD-4** 공지 템플릿 3종 — `PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0` + `PROD_LAUNCH_CHECKLIST_v1.0` + 베타→정식 전환 안내 (이메일 + 사내 메신저 2종) + `USER_MANUAL v1.5 §3.7 Blue/Green 가이드` 신규 단락.
3. **S25-B carry-over (~2.5 SP, Phase 5+ 베타 28일 마감 + Go/No-Go 후)**:
   - **ST-PROD-1** 사내 IT Keycloak realm 활성 + LDAP/AD sync 실 endpoint + 30명 실 발급 + 첫 로그인 PIN 강제 변경 시뮬 검증.
   - **ST-PROD-2** STG 실 부하 측정 (Docker stack + 47 품번 seed + 30 JWT 발급 + 5분 × 4 시나리오) + 결과 분석 + 튜닝 + LOAD_TEST_REPORT v1.0.
   - **ST-PROD-3** 실 STG → PROD switch (Blue → Green 첫 deploy) + in-flight 요청 손실 0건 검증 + BLUE_GREEN_RUNBOOK v1.0.
   - **ST-PROD-4** 실 공지 발송 (메일 30명) + 베타 8명 → 정식 30명+ 전환 + Phase 4 closing addendum (WBS v1.21).
4. **DoD 갱신 (10 → 7)** — S25-A 한정 — 실 부하/실 switch/실 발급 등 사내 IT/STG 의존 항목은 S25-B 로 이동.
5. **리스크 5 → 5 (early-entry 신규)** — Blue/Green fix 실 deploy 검증 0 / k6 script 실 endpoint 검증 0 / Keycloak seed 실 realm 검증 0 / 공지 템플릿 사내 IT 협의 미반영 / V05x seed 사번 placeholder ↔ 실 명단 충돌.

---

## 1. 목적 (early-entry 분할 명시)

**v1.0 정식 진입 조건 (베타 28일 완주 + BETA_REPORT v1.0 Go + 사내 IT Keycloak 활성 + 30명 명단 확정) 미충족 → S25-A 조기 진입으로 script/fix/seed/템플릿 차원 data-free 작업만 수행.** 베타 4주 완주 + Go 결정 + 사내 IT 협의 완료 시점에 S25-B 정식 진입.

| 영역 | v1.0 baseline | **S25-A 조기 (지금)** | **S25-B carry-over (Phase 5+)** |
|---|---|---|---|
| Keycloak 사용자 | 0명 | **V05x__seed_prod_users.sql 30명 idempotent** + prod-users-table v1.0 | **실 realm 활성 + LDAP sync + 30명 발급 + PIN 변경 검증** |
| k6 부하 | 없음 | **load-test-prod.js script + 4 시나리오 + threshold** | **STG 실 측정 + 튜닝 + LOAD_TEST_REPORT v1.0** |
| Blue/Green | Sprint 19 stub | **deploy.sh + compose + rollback.sh 정합 fix 3건 + DRY_RUN** | **실 switch (B→G) + in-flight 0건 + BLUE_GREEN_RUNBOOK v1.0** |
| 공지 + 매뉴얼 | 없음 | **공지 템플릿 3 + USER_MANUAL v1.5 §3.7 Blue/Green** | **실 공지 발송 + 베타→정식 전환 + WBS v1.21 closing** |

**S25-A 진입 효과:** 베타 28일 운영 동안 script/fix/seed/템플릿 선행 → S25-B 진입 시 정식 작업이 실 환경 적용/측정/발송 만으로 단축. Blue/Green 정합성 fix 는 코드 리뷰 차원 선행 가능 (실 deploy 무관). PLAN-SPRINT-24 v1.1 (`fafa2db`) 분할 패턴 재사용.

---

## 2. Sprint 25-A SP·기간 (1.5 SP / 0.7 PD / 1 Day)

| Story | S25-A SP | S25-B 잔여 | PD |
|---|:--:|:--:|:--:|
| ST-PROD-1 Keycloak V05x seed 30명 + prod-users 문서 | 0.3 | 0.7 (실 realm + LDAP + 30명 발급) | 0.15 |
| ST-PROD-2 k6 script + threshold 4 시나리오 | 0.5 | 1.0 (STG 실 측정 + 튜닝 + 보고서) | 0.25 |
| ST-PROD-3 Blue/Green 정합성 fix 3건 + DRY_RUN | 0.5 | 0.5 (실 switch + 검증 + Runbook) | 0.2 |
| ST-PROD-4 공지 템플릿 3 + USER_MANUAL v1.5 §3.7 | 0.2 | 0.3 (실 발송 + 전환 + WBS closing) | 0.1 |
| **S25-A 합계** | **~1.5 SP** | **~2.5 SP (S25-B)** | **~0.7 PD** |

---

## 3. 의존성 DAG (S25-A 5 task 독립 병렬)

```
S25-A (지금, Pre-Phase 18% 충족 — script/fix/seed/템플릿만)
  ├─ ST-PROD-2 k6 load-test-prod.js (TK-2-1, 2-2)     ─┐
  ├─ ST-PROD-3 deploy.sh + compose + rollback fix      │
  │           (TK-3-1, 3-2, 3-3)                       ├─ 독립 병렬 가능
  ├─ ST-PROD-1 V05x seed + prod-users 문서             │  (5 task DAG 교차 없음)
  │           (TK-1-1, 1-2)                            │
  ├─ ST-PROD-4 공지 템플릿 3 (TK-4-1, 4-2)             │
  └─ ST-PROD-4 USER_MANUAL v1.5 §3.7 (TK-4-3)         ┘
                  ↓
            S25-A 산출물 commit/push (gitflow-commit)
                  ↓
 ━━━ 베타 28일 완주 + BETA_REPORT v1.0 Go + 사내 IT Keycloak + 30명 명단 확정 ━━━
                  ↓
S25-B (Phase 5+, 정식 진입)
  ├─ ST-PROD-1 실 Keycloak realm + LDAP sync + 30명 발급 + PIN 검증
  ├─ ST-PROD-2 STG 실 부하 측정 + 튜닝 + LOAD_TEST_REPORT v1.0
  ├─ ST-PROD-3 실 STG→PROD switch (B→G) + BLUE_GREEN_RUNBOOK v1.0
  └─ ST-PROD-4 실 공지 발송 + 베타→정식 전환 + WBS v1.21 closing
                  ↓
            Phase 4 종료 → Phase 5+ 본격 운영
```

---

## 4. Story · Task 매트릭스 (TK 별 S25-A / S25-B 라벨)

### ST-PROD-1 — 사용자 확장 30명+

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-PROD-1-1 | **Keycloak `V05x__seed_prod_users.sql` 신규** — 30명 (PLANNER 10 + STK 15 + IT_OPS 3 + READ 5), V037 패턴 idempotent `ON CONFLICT (username) DO NOTHING`, BCrypt strength 12, 사번 placeholder `EMP0XXXX` | **S25-A** | 0.15 |
| TK-PROD-1-2 | **`docs/cutover/PROD_USERS_TABLE_v1.0.md`** — 30명 명단 표 (사번/role/email placeholder/부서) + 사내 IT 협의 가이드 | **S25-A** | 0.15 |
| TK-PROD-1-3 | 사내 IT Keycloak realm `scheduling` SAML/OIDC 활성 + LDAP/AD sync 실 endpoint | **S25-B** | 0.3 |
| TK-PROD-1-4 | 30명 실 발급 + 첫 로그인 PIN 강제 변경 (Sprint 22 정합) 시뮬 검증 + AppUser sync IT | **S25-B** | 0.4 |

### ST-PROD-2 — k6 부하 검증

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-PROD-2-1 | **`infrastructure/k6/load-test-prod.js` 신규** — 30 VU × 5분 × 4 시나리오 (login + 시뮬뷰 조회 + Diff GET + 확정), `setup()` JWT placeholder | **S25-A** | 0.3 |
| TK-PROD-2-2 | **threshold 정의** — `http_req_duration p(95)<800` + `http_req_failed rate<0.001` (REQ-NF-PER-004) + scenario stages (ramp-up 30s/steady 4m/ramp-down 30s) | **S25-A** | 0.2 |
| TK-PROD-2-3 | STG 실 측정 — Docker stack 기동 + 47 품번 seed + 30 JWT 발급 + 5분 실행 | **S25-B** | 0.4 |
| TK-PROD-2-4 | 결과 분석 → HikariCP / JVM heap / Postgres slow query 튜닝 + Grafana 패널 신규 | **S25-B** | 0.4 |
| TK-PROD-2-5 | **`docs/cutover/LOAD_TEST_REPORT_v1.0.md`** — p50/p95/p99 + RPS + error rate + 튜닝 액션 + Go/No-Go | **S25-B** | 0.2 |

### ST-PROD-3 — Blue/Green 무중단 deploy

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-PROD-3-1 | **`infrastructure/scripts/deploy.sh` 정합 fix** — NGINX 경로 `prod-active.conf` 정합 + `BACKEND_IMAGE` → `TAG_BLUE`/`TAG_GREEN` 환경변수 분리 + `DRY_RUN=1` 분기 (실 docker push/up 차단) | **S25-A** | 0.2 |
| TK-PROD-3-2 | **`infrastructure/docker-compose.prod.yml` fix** — `backend-green` `depends_on` healthcheck `condition: service_healthy` 추가 + readiness probe 정합 | **S25-A** | 0.15 |
| TK-PROD-3-3 | **`infrastructure/scripts/rollback.sh` smoke test** — readiness `/actuator/health/readiness` 1회 + 실패 시 NGINX upstream 즉시 회귀 분기 | **S25-A** | 0.15 |
| TK-PROD-3-4 | STG 환경 실 switch (Blue → Green) + in-flight 요청 0건 손실 검증 (k6 30 VU 동시) | **S25-B** | 0.3 |
| TK-PROD-3-5 | **`docs/cutover/BLUE_GREEN_RUNBOOK_v1.0.md`** — 단계별 절차 + rollback + 1주 hotline | **S25-B** | 0.2 |

### ST-PROD-4 — PROD 운영 시작 공지

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-PROD-4-1 | **`docs/cutover/PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0.md`** — 사내 메신저 공지 + 이메일 (시작 일시/URL/매뉴얼 link/1주 hotline IT_OPS) 2종 | **S25-A** | 0.08 |
| TK-PROD-4-2 | **`docs/cutover/PROD_LAUNCH_CHECKLIST_v1.0.md`** — T-1주 / T-1일 / T-0 / T+1일 / T+1주 checklist + 베타→정식 전환 안내 단락 | **S25-A** | 0.07 |
| TK-PROD-4-3 | **`docs/manual/USER_MANUAL_v1.5.md` §3.7 Blue/Green 가이드** 신규 단락 — 사용자 입장 무중단 deploy 안내 + 우회 절차 | **S25-A** | 0.05 |
| TK-PROD-4-4 | 실 공지 발송 (메일 30명) + 사내 메신저 + 베타 8명 → 정식 30명+ migration 안내 | **S25-B** | 0.2 |
| TK-PROD-4-5 | Phase 4 closing addendum (WBS v1.21) + Phase 5+ carry-over 6 항목 우선순위 명시 | **S25-B** | 0.1 |

---

## 5. Definition of Done (S25-A 한정 7건)

1. ✅ k6 `load-test-prod.js` 신규 — 30 VU × 5분 × 4 시나리오 + threshold (p95<800ms / error<0.1%) 코드 GREEN
2. ✅ Blue/Green 정합성 fix 3건 — `deploy.sh` (NGINX 경로 + TAG 분리 + DRY_RUN) + `docker-compose.prod.yml` (depends_on health) + `rollback.sh` (readiness smoke) shellcheck GREEN
3. ✅ Keycloak `V05x__seed_prod_users.sql` 신규 — 30명 idempotent + BCrypt 12 + Flyway validate GREEN (placeholder 사번)
4. ✅ `PROD_USERS_TABLE_v1.0.md` 발행 — 30명 명단 표 + 사내 IT 협의 가이드
5. ✅ 공지 템플릿 3종 (ANNOUNCEMENT_TEMPLATE + CHECKLIST + 베타→정식 전환) 발행
6. ✅ `USER_MANUAL v1.5 §3.7` Blue/Green 가이드 신규 단락
7. ✅ ArchUnit GREEN + Backend IT 회귀 0 + Flyway migration validate GREEN

---

## 6. 리스크 + 회피 (early-entry 신규)

| 리스크 | 영향 | 회피 |
|---|---|---|
| **Blue/Green fix 실 deploy 검증 0** — `deploy.sh` / `compose` / `rollback.sh` 정합 fix 가 실 STG 환경 미검증 | 실 switch 시 회귀 | S25-A `DRY_RUN=1` 분기 + shellcheck + docker-compose config validate. S25-B 실 STG switch 시 1차 회귀 검증 |
| **k6 script 실 endpoint 검증 0** — `load-test-prod.js` 가 실 JWT/실 시뮬뷰/실 Diff endpoint 응답 미확인 | 실 측정 시 NoGo | S25-A endpoint placeholder + scenario stages 명시 + JWT setup() 함수 stub. S25-B 실 30 JWT + 실 STG 1차 실행 |
| **Keycloak seed 실 realm 검증 0** — `V05x` migration 이 실 realm 활성 전 검증 불가 | 30명 발급 충돌 | S25-A `ON CONFLICT DO NOTHING` idempotent + placeholder 사번 + BCrypt strength 12 검증. S25-B 사내 IT 명단 확정 후 실 사번 SQL UPDATE 1회 |
| **공지 템플릿 사내 IT 협의 미반영** — ANNOUNCEMENT/CHECKLIST 가 사내 메신저 표준/이메일 표준 미반영 | 공지 효과 ↓ | S25-A 템플릿 형식 markdown + 사내 IT 협의 점검표 명시 (placeholder). S25-B 실 발송 전 사내 IT 1차 검토 |
| **V05x seed 사번 placeholder ↔ 실 명단 충돌** — 30명 실 명단이 placeholder 사번과 다를 경우 SQL 재작성 | migration 회귀 | S25-A `EMP0XXXX` 명시 placeholder + 주석 "S25-B 진입 시 실 사번 일괄 치환" + `PROD_USERS_TABLE_v1.0` 매핑 표 |

---

## 7. 작업 순서 추천 (1 Day S25-A 일괄, Workflow 패턴)

**Day 1 (~0.7 PD, 5~6시간):**
1. (오전) **TK-PROD-2-1, 2-2** k6 `load-test-prod.js` + threshold (백엔드 병렬)
2. (오전) **TK-PROD-3-1, 3-2, 3-3** Blue/Green fix 3건 (`deploy.sh` + `compose` + `rollback.sh`) — shellcheck GREEN
3. (오후) **TK-PROD-1-1, 1-2** Keycloak V05x seed 30명 + PROD_USERS_TABLE v1.0 (Flyway validate GREEN)
4. (오후) **TK-PROD-4-1, 4-2** 공지 템플릿 2종 (ANNOUNCEMENT + CHECKLIST)
5. (마무리) **TK-PROD-4-3** USER_MANUAL v1.5 §3.7 Blue/Green 단락 + commit/push (gitflow-commit)

**Workflow 패턴 재사용:** Sprint 24 v1.1 1 Day 일괄 패턴 (`fafa2db`) 그대로 — 5 task 독립 병렬 + 마무리 commit/push.

---

## 8. 산출물 (Deliverables)

| 분류 | S25-A 파일 |
|---|---|
| Plan | `Phase 2/4.Tasks/PLAN-SPRINT-25_EP-PROD-LAUNCH_v1.1.md` |
| Infra k6 | `infrastructure/k6/load-test-prod.js` (30 VU × 5분 × 4 시나리오 + threshold) |
| Infra Scripts | `infrastructure/scripts/deploy.sh` (NGINX 경로 + TAG_BLUE/GREEN + DRY_RUN) + `infrastructure/scripts/rollback.sh` (readiness smoke) |
| Infra Compose | `infrastructure/docker-compose.prod.yml` (backend-green depends_on health) |
| Backend Migration | `backend/src/main/resources/db/migration/V05x__seed_prod_users.sql` (30명 idempotent BCrypt 12) |
| Docs Cutover | `docs/cutover/PROD_USERS_TABLE_v1.0.md` + `docs/cutover/PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0.md` + `docs/cutover/PROD_LAUNCH_CHECKLIST_v1.0.md` |
| Docs Manual | `docs/manual/USER_MANUAL_v1.5.md` §3.7 Blue/Green 가이드 신규 단락 |

---

## 9. Sprint 25-B 진입 조건 (다음 단계)

**S25-B 정식 진입 게이트:**
- ✅ 베타 28일 완주 (운영 누적 100% — 현재 ~5일/18%)
- ✅ BETA_REPORT v1.0 발행 + Go 결정 (KPI + 인터뷰 8명 + 12+ issue resolution)
- ✅ 사내 IT Keycloak realm `scheduling` 활성 + LDAP/AD sync endpoint 확정
- ✅ 30명+ 실 명단 확정 (사번/role/email/부서) + 사내 관리팀 승인
- ⏳ S25-A 산출물 7건 DoD 충족 + commit/push merge

**S25-B 진입 시:**
- ST-PROD-1 사내 IT Keycloak realm 활성 + LDAP sync 실 endpoint + 30명 실 발급 + 첫 로그인 PIN 변경 검증
- ST-PROD-2 STG 실 부하 측정 + 튜닝 + LOAD_TEST_REPORT v1.0
- ST-PROD-3 실 STG → PROD switch (Blue → Green 첫 deploy) + in-flight 0건 + BLUE_GREEN_RUNBOOK v1.0
- ST-PROD-4 실 공지 발송 + 베타 8명 → 정식 30명+ 전환 + Phase 4 closing addendum (WBS v1.21)

**S25-B 후 Phase 5+ carry-over:** MES MQ adapter / MES file adapter / Order auto-INSERT chain / ML priority / multi-tenant / 30명+ 인덱스 튜닝 (v1.0 §9 6 항목 그대로).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 마지막 sprint EP-PROD-LAUNCH 4 Story / 15 Task / ~4 SP 분해. 베타 8명 → PROD 30명+ 확장 + k6 부하 + Blue/Green 무중단 deploy + 공식 운영 진입. DoD 10 + 리스크 5. Pre-Phase 베타 보고서 Go + Keycloak sync 준비 + 30명 명단. **Phase 4 종료 → Phase 5+ 본격 운영 진입**. Phase 5+ carry-over 6 항목 식별. |
| **1.1** | **2026-06-01** | **Claude Code** | **조기 진입 모드 — S25-A / S25-B 분할.** Pre-Phase 18% 충족 (베타 ~5일/28일, 사용자 발급 0/8, BETA_REPORT v0.1 골격만 — Go/No-Go 데이터 0%) → 정식 4 SP 일괄 진입 불가. **S25-A ~1.5 SP (0.7 PD / 1 Day)** — data-free: k6 `load-test-prod.js` script (30 VU × 5분 × 4 시나리오 + threshold) + Blue/Green 정합성 fix 3건 (deploy.sh + compose + rollback.sh + DRY_RUN) + Keycloak `V05x__seed_prod_users.sql` 30명 idempotent + PROD_USERS_TABLE v1.0 + 공지 템플릿 3종 (ANNOUNCEMENT + CHECKLIST + 베타→정식 전환) + USER_MANUAL v1.5 §3.7 Blue/Green 가이드. **S25-B ~2.5 SP carry-over** (Phase 5+ 베타 28일 + Go/No-Go 후) — 실 Keycloak realm + LDAP sync + 30명 발급 + STG 실 부하 측정 + 튜닝 + LOAD_TEST_REPORT v1.0 + 실 B→G switch + BLUE_GREEN_RUNBOOK v1.0 + 실 공지 발송 + WBS v1.21 closing. DoD 10 → 7 (S25-A 한정). 리스크 5 → 5 (early-entry 신규: Blue/Green fix 실 검증 0 / k6 endpoint 검증 0 / Keycloak realm 검증 0 / 공지 사내 IT 미반영 / V05x placeholder ↔ 실 명단 충돌). 작업 순서 3 Day → 1 Day S25-A 일괄 (Sprint 24 v1.1 `fafa2db` Workflow 패턴 재사용). S25-B 진입 게이트 4 항목 (베타 완주 + BETA_REPORT v1.0 Go + 사내 IT Keycloak + 30명 명단). |
