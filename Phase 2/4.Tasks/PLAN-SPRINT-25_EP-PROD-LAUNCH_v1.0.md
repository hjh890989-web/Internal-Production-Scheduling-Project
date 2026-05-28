# Sprint 25 진입 계획 — EP-PROD-LAUNCH (본격 운영 진입) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 마지막 sprint 진입 권고안 (S20~S24 완료 후 + 베타 보고서 Go)

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S25](PHASE-4_STABILIZATION_v1.0.md) + [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md) + 베타 보고서 v1.0 Go 결정

---

## 1. 목적

**Phase 4 운영 안정화 완료 — 베타 8명 → 본격 운영 30명+ 확장 + 부하 검증 + Blue/Green 무중단 deploy + 베타 → PROD 공식 전환.**

| 항목 | Sprint 19 baseline (베타 8명) | Sprint 25 활성 (PROD 30명+) |
|---|---|---|
| 사용자 수 | 8명 (PLANNER 3 + STK 3 + IT_OPS 1 + READ 1) | ✅ **30명+** (PLANNER 10 + STK 15 + IT_OPS 3 + READ 5) |
| 인증 sync | 수동 V037 seed | ✅ **Keycloak SAML/OIDC + 사내 LDAP/AD sync** (Phase 2 EP-30 활성) |
| 부하 검증 | E2E IT 1회 | ✅ **k6 부하 검증** — 30 동시 사용자 + p95 < 800ms |
| Deploy | NSSM 단일 backend | ✅ **Blue/Green 무중단 deploy** (NGINX upstream switch) |
| 운영 상태 | 베타 (사내 8명 한정) | ✅ **PROD 공식** (사내 전사 공지 + 매뉴얼 배포) |

**Pre-Phase 의존 (Sprint 25 진입 전 필수):**
- 베타 보고서 v1.0 Go 결정 (Sprint 24 종료)
- 사내 IT — Keycloak SAML/OIDC + LDAP sync 준비 완료
- 사내 관리팀 — 30명+ 사용자 사번/role 명단 확정

**활성 후 효과:**
- 사내 전사 (생산 계획 + 현장 STK + IT_OPS + 임원) 동시 사용
- PROD 무중단 deploy → 베타 운영 hours 차단 시간 0
- Phase 5+ (본격 운영 안정화) 진입 → MES MQ adapter / Order auto-INSERT chain / multi-tenant 등

---

## 2. Sprint 25 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-PROD-1 사용자 확장 30명+ (Keycloak SAML/OIDC + LDAP sync) | 1.0 | 0.5 |
| ST-PROD-2 k6 부하 검증 (30 동시 사용자 + p95 < 800ms) | 1.5 | 0.8 |
| ST-PROD-3 Blue/Green 무중단 deploy (NGINX upstream switch 검증) | 1.0 | 0.5 |
| ST-PROD-4 PROD 운영 시작 공지 + 베타 마감 | 0.5 | 0.2 |
| **합계** | **~4 SP** | **~2 PD** |

---

## 3. 의존성 DAG

```
Pre-Phase (베타 Go + Keycloak sync 준비 + 30명 명단)
    ↓
ST-PROD-1 (사용자 확장) ──┐
                         │
ST-PROD-2 (k6 부하 검증) ─┤  (각 독립)
                         │
ST-PROD-3 (Blue/Green) ──┘
                         ↓
                ST-PROD-4 (공지 + 마감)
                         ↓
              Phase 5+ (본격 운영 안정화)
```

---

## 4. Story · Task 매트릭스

### ST-PROD-1 — 사용자 확장 30명+ (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-PROD-1-1 | Keycloak realm `scheduling` SAML/OIDC 활성 (Phase 2 EP-30 baseline) + 사내 LDAP/AD sync 1회 | 0.3 |
| TK-PROD-1-2 | 사용자 명단 import — CSV (사번 + role) → Keycloak bulk insert + AppUser 동기화 service | 0.3 |
| TK-PROD-1-3 | 30명+ 첫 로그인 시 PIN 강제 변경 흐름 (Sprint 22 정합) | 0.2 |
| TK-PROD-1-4 | IT — Keycloak Mock 30명 시뮬 + AppUser sync + 첫 로그인 PIN 변경 검증 | 0.2 |

### ST-PROD-2 — k6 부하 검증 (1.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-PROD-2-1 | `infrastructure/k6/load-test-prod.js` — 30 가상 사용자 × 5분 (login + 시뮬뷰 조회 + Diff GET + 확정 시나리오) | 0.5 |
| TK-PROD-2-2 | k6 측정 — p50/p95/p99 latency + RPS + error rate. PASS 기준: p95 < 800ms (REQ-NF-PER-004) + error rate < 0.1% | 0.3 |
| TK-PROD-2-3 | 부하 결과 분석 → HikariCP pool / JVM heap / Postgres slow query 튜닝 (필요 시) | 0.3 |
| TK-PROD-2-4 | Grafana 패널 추가 — k6 결과 시각화 (response time histogram + RPS gauge) | 0.2 |
| TK-PROD-2-5 | `docs/cutover/LOAD_TEST_REPORT_v1.0.md` — k6 결과 + 튜닝 액션 + PROD 안정성 검증 보고 | 0.2 |

### ST-PROD-3 — Blue/Green 무중단 deploy (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-PROD-3-1 | `infrastructure/scripts/blue-green-switch.sh` 검증 (Sprint 19 stub → 실 동작 검증) — NGINX upstream toggle | 0.3 |
| TK-PROD-3-2 | docker-compose.prod.yml — backend-blue 활성 + backend-green standby + healthcheck readiness | 0.3 |
| TK-PROD-3-3 | switch 시뮬 1회 — STG 환경에서 blue → green 전환 + 사용자 in-flight 요청 0건 손실 검증 | 0.2 |
| TK-PROD-3-4 | `docs/cutover/BLUE_GREEN_RUNBOOK_v1.0.md` — 무중단 deploy 단계별 절차 + rollback | 0.2 |

### ST-PROD-4 — PROD 운영 시작 공지 + 베타 마감 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-PROD-4-1 | `docs/cutover/PROD_LAUNCH_ANNOUNCEMENT_v1.0.md` — 사내 메신저 공지문 (시작 일시 + URL + 매뉴얼 link + 1주 hotline IT_OPS) | 0.2 |
| TK-PROD-4-2 | 베타 사용자 8명 → 본격 사용자 30명 migration (PIN reset + 첫 로그인 가이드 1회 메일) | 0.2 |
| TK-PROD-4-3 | Phase 4 종료 + Phase 5+ carry-over 정리 (WBS v1.21 Phase 4 closing addendum) | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ Keycloak SAML/OIDC + LDAP sync 활성 + 30명+ 첫 로그인 모두 성공
2. ✅ k6 부하 검증 — 30 동시 사용자 + p95 < 800ms + error rate < 0.1%
3. ✅ HikariCP / JVM heap / Postgres 튜닝 액션 보고 (필요 시 적용)
4. ✅ Blue/Green switch 1회 시뮬 — in-flight 요청 0건 손실
5. ✅ 사내 PROD 운영 시작 공지 + 30명 메일 발송
6. ✅ Phase 4 closing 정리 (WBS v1.21 addendum)

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend IT 회귀 0 (Keycloak Mock 신규)
3. ✅ k6 부하 결과 PASS 보고서 발행
4. ✅ Phase 5+ carry-over 식별 명확화 (MES MQ / Order auto-INSERT / multi-tenant)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| Keycloak LDAP sync 30명 충돌 (사번 중복 / role 매핑 오류) | 첫 로그인 다수 실패 | 사전 STG 환경 sync 1회 검증 + 30명 명단 cross-check |
| k6 부하 검증 p95 SLA 위반 (Sprint 19 1500 row × 30 col baseline) | PROD 진입 NoGo | HikariCP active 30 → 50 확장 + Postgres connection limit 100 + Phase 5+ 인덱스 추가 |
| Blue/Green switch in-flight 요청 손실 | 무중관 deploy 실패 | NGINX `proxy_next_upstream off` + healthcheck 5초 + graceful shutdown 30초 |
| 30명 동시 첫 로그인 → PIN 변경 폭주 | UI freeze | 단계적 안내 (10명 × 3 일) + IT_OPS hotline 1주 |
| Phase 4 carry-over 누락 → Phase 5 진입 spec 불명확 | 다음 phase 지연 | WBS v1.21 closing addendum 에 Phase 5+ 5+ 항목 우선순위 명시 |

---

## 7. 작업 순서 추천

**Day 1** — Keycloak + k6 (병렬):
1. TK-PROD-1-1~4 (Keycloak SAML/OIDC + 30명 sync)
2. TK-PROD-2-1~3 (k6 부하 + 분석)

**Day 2** — Blue/Green + 튜닝:
3. TK-PROD-3-1~4 (Blue/Green switch + runbook)
4. TK-PROD-2-4~5 (Grafana + 보고서)

**Day 3** — PROD 공지 + 베타 마감:
5. TK-PROD-4-1~3 (공지 + 마감 + WBS closing)
6. **DoD 본 PC 시각 검증** — Blue/Green switch 1회 + k6 부하 1회 + 30명 첫 로그인 시뮬

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Service | UserSyncService (Keycloak → AppUser) + 첫 로그인 PIN 강제 변경 (Sprint 22 정합) |
| Backend IT | KeycloakUserSyncIT (30명 시뮬) + 회귀 |
| Infra | k6/load-test-prod.js + blue-green-switch.sh 검증 + docker-compose.prod.yml (blue/green) |
| Docs Cutover | LOAD_TEST_REPORT_v1.0 + BLUE_GREEN_RUNBOOK_v1.0 + PROD_LAUNCH_ANNOUNCEMENT_v1.0 |
| WBS | TASK-001_WBS_v1.21 (Phase 4 closing addendum + Phase 5+ carry-over) |

---

## 9. Sprint 25 후 다음 단계 — Phase 5+ 본격 운영

**Phase 4 종료 시점 — 본격 운영 시작:**
- 사내 전사 사용 (30명+ → 사용량 추세에 따라 50명+ 확장 가능)
- Grafana 실 운영 모니터링 + IT_OPS 1차 대응
- 사용자 매뉴얼 v1.5 + 베타 보고서 정합

**Phase 5+ carry-over (Phase 4 closing addendum 에 명시):**
1. MES MQ adapter (RabbitMQ / Kafka — 벤더 결정 후) — High
2. MES file adapter (legacy 호환 CSV / Excel polling) — High
3. Order 자동 INSERT chain (ImportOrchestrator → Allocator) — Medium
4. ML/AI 기반 priority 알고리즘 (PRODUCT_PRIORITY 자동 갱신) — Low (6개월 데이터 후)
5. Multi-tenant 확장 (다른 공장) — Low (사내 multi-plant 결정 시)
6. 30+ 사용자 확장 후 인덱스/쿼리 튜닝 — Medium (k6 결과 기반)

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 마지막 sprint EP-PROD-LAUNCH 4 Story / 15 Task / ~4 SP 분해. 베타 8명 → PROD 30명+ 확장 + k6 부하 + Blue/Green 무중단 deploy + 공식 운영 진입. DoD 10 + 리스크 5. Pre-Phase 베타 보고서 Go + Keycloak sync 준비 + 30명 명단. **Phase 4 종료 → Phase 5+ 본격 운영 진입**. Phase 5+ carry-over 6 항목 식별 (MES MQ/file / Order auto-INSERT / ML priority / multi-tenant / 부하 튜닝). |
