# Phase 4 (베타 운영) 진입 계획 v1.0

**Phase**: 4 (베타 운영) | **목표 기간**: 2026-05-24 ~ 2026-06-30 (~5주)
**상태**: 🔄 진입 게이트 | **작성**: 2026-05-23
**상위 참조**: [Phase-3_Completion_v1.0.md](../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.0.md)

> Phase 3 (개발) Sprint 0~6 = 7 Sprint × 47 Epic × ~287 Task × 153 commit × 9 영업일 완료.
> **Phase 4 = STG 베타 운영 + 사내 IdP 통합 + DR + 알림 + 사용자 교육**. 5주 후 PROD cutover.

---

## 1. Phase 4 목표

> "사내 사용자 (~10명) 대상 베타 운영. PROD cutover 전 5주 stress test + 사용자 피드백 +
>  성능 NFR 실 측정 + DR 검증."

핵심 마일스톤 — STG 부팅 + Keycloak SSO + 베타 5 시나리오 + k6 실 측정 + PROD cutover 게이트.

---

## 2. 5 phase 마일스톤

| Phase | 기간 | 항목 | 종료 게이트 |
|---|---|---|---|
| **4-A** STG 부팅 | Week 1 (5d) | Docker Compose Blue/Green STG + Keycloak SSO + 시드 데이터 + k6 실 측정 | STG 가동 + IT_OPS 접근 |
| **4-B** 베타 시나리오 | Week 2 (5d) | 5 사용자 시나리오 (정상·예외·복원·override·cascade) | 시나리오 100% 통과 |
| **4-C** 사용자 교육 | Week 3 (5d) | Planner / STK_USER 페르소나 별 운영 매뉴얼 + 영상 + Q&A | 4 페르소나 교육 |
| **4-D** DR + 보안 검증 | Week 4 (5d) | pg_basebackup PITR + 사내 IdP LDAP/AD sync + Alertmanager Slack | DR 시나리오 통과 |
| **4-E** PROD 진입 결정 | Week 5 (5d) | Cutover 게이트 회의 + 운영팀 인수 + Phase 5 (PROD) 진입 | 승인 |

---

## 3. Phase 4 진입 게이트 충족 (Phase 3 완료 → Phase 4 진입)

- [x] Phase 3 Sprint 0~6 100% 완료
- [x] 47 Epic 거버넌스 + 안정성 + 관측성 완비
- [x] 9 Modulith 모듈 + 33 Flyway + 19 KPI 영속
- [x] 153 commit · 머지 충돌 0 · AI harness 안정
- [x] Backend 회귀 249 tests + Frontend 54 + Playwright 226 등록

→ **Phase 4 진입 승인 가능**. 운영팀 + IT_OPS + Planner 페르소나 인계.

---

## 4. STG 환경 명세 (Phase 4-A)

### Docker Compose Blue/Green

```
infra/docker-compose.stg.yml (신규)
  ├ scheduling-app-blue   (8080)   — 현재 활성 instance
  ├ scheduling-app-green  (8081)   — 대기 (배포 시 스위치)
  ├ postgres              (5432)   — 운영 DB (pg_basebackup + WAL)
  ├ redis                 (6379)   — STOMP fan-out + 캐시
  ├ keycloak              (8090)   — 사내 IdP (LDAP/AD sync)
  ├ prometheus            (9090)
  ├ grafana               (3000)
  ├ loki                  (3100)
  ├ promtail
  └ nginx                 (443)    — TLS + Blue/Green upstream toggle
```

### env var (STG)

```bash
KEYCLOAK_ISSUER_URI=https://keycloak.intranet/realms/scheduling
KEYCLOAK_JWKS_URI=https://keycloak.intranet/realms/scheduling/protocol/openid-connect/certs
KAKAO_ENABLED=true
KAKAO_WEBHOOK_URL=https://workplace-bot.intranet/...
KAKAO_BOT_TOKEN=${사내 발급}
VITE_AG_GRID_LICENSE_KEY=${운영 라이센스}
POSTGRES_HOST=postgres
REDIS_HOST=redis
APP_URL=https://schedule.intranet
```

---

## 5. 5 베타 시나리오 (Phase 4-B)

| # | 시나리오 | 페르소나 | 검증 |
|---|---|---|---|
| 1 | **정상 1주 horizon** — 수주 import → VC 자동 스케줄 → Planner confirm → EX cascade | Planner | 1500 row 30 col 정상 생성 + Excel export 동일 |
| 2 | **충돌 + alternative** — 마스터 충돌 입력 → ≥3 distinct alternative + Planner 선택 | Planner + STK_USER | 충돌 분류 100% + ranking 정렬 |
| 3 | **VC 변경 cascade** — Planner override → vc.changed → ex partial replan → STOMP push → 매트릭스 갱신 | Planner | p95 ≤ 2초 + audit 자동 |
| 4 | **마스터 복원** — 잘못된 vc_constraint 입력 → audit timeline forensic → 시점 snapshot 확인 | IT_OPS | JSONB 역재생 정확 |
| 5 | **일중 락 override** — BR-V07 위반 시도 → trigger reject → override 사유 입력 → audit 캡쳐 | Planner | reason+actor 강제 + DO-04 영업일 키 |

---

## 6. DR + 보안 검증 (Phase 4-D)

### DR 시나리오

```
1. pg_basebackup full backup (매주 일요일 02:00 NAS push)
2. WAL archiving (15분 간격)
3. PITR 시나리오 — 어제 14:30 시점 복원 시뮬
4. spring-modulith event_publication 미완료 publication 재시작 복구
5. audit.schedule_audit_log 월별 partition 1개 검증 (drop 후 PITR 복원)
```

### 보안 검증

```
1. Keycloak LDAP/AD sync (사내 디렉터리 통합)
2. SAML 2.0 SP 등록 (Phase 2+ 옵션 — OIDC 우선)
3. JWT 만료 갱신 + Refresh token rotation
4. RBAC 4 role 매트릭스 회귀 (PLANNER / STK_USER / IT_OPS / READ_ONLY)
5. Audit immutable 검증 — DBA 권한도 UPDATE/DELETE 거부 (V026 trigger)
6. NFR-SEC-007 사번 8자리 + PIN 4자리 + 5회/10분 잠금 (Sprint 4 v1.5)
```

---

## 7. PROD Cutover 게이트 (Phase 4-E)

| 영역 | 게이트 | 측정 |
|---|---|---|
| **베타 5 시나리오** | 100% 통과 | 1주 회귀 |
| **k6 STG 실 측정** | matrix p95 < 800ms, ranking < 1200ms | 100 user × 5분 |
| **AG Grid 1500 row** | first render < 500ms | Lighthouse FCP |
| **사용자 교육** | 4 페르소나 100% | Q&A 통과 |
| **DR PITR** | 시점 복원 정확 | 시나리오 |
| **알림 도달률 (NS-S04)** | ≥ 95% (Kakao + Email + Slack) | Grafana 1주 |
| **BR-V07 일중 락 위반** | 0건 | 1주 회귀 |
| **NS-S07 D-1 준수율** | ≥ 98% | KPI 측정 |
| **NS-S09 신규 라인** | ≥ 90% | KPI 측정 |
| **Modulith verify** | 0 위반 | 부팅 시 |
| **회귀 (Backend + FE + Playwright)** | 0 failure | CI |

---

## 8. Phase 4 후속 — Phase 5 (PROD) 진입 사전 작업

| 항목 | 책임 | 마감 |
|---|---|---|
| PROD Docker Compose + Blue/Green | IT_OPS | 4-E |
| PROD Keycloak realm 분리 | IT_OPS | 4-E |
| 사내 NAS S3 호환 (Excel 첨부 영속) | IT_OPS | Phase 5 |
| Sentry APM 통합 (frontend + backend) | IT_OPS | Phase 5 |
| 사용자 운영 매뉴얼 + 변경 관리 가이드 | STK-08 | 4-E |
| 비상 대응 절차 (RPO 15분 / RTO 1시간) | IT_OPS | 4-D |

---

## 9. 위험 + 완화 전략

| 리스크 | 영향 | 완화 |
|---|---|---|
| Keycloak SAML 통합 실패 | 인증 차단 | local fallback (사내 임시 ID 매핑) + OIDC 우선 |
| k6 STG 실 측정 p95 미달 | PROD 진입 차단 | QueryDSL projection + EntityGraph N+1 진단 |
| 베타 사용자 학습 곡선 | Sprint 5 UI 사용도 ↓ | 페르소나별 영상 + Q&A 세션 × 4회 |
| pg_basebackup NAS 용량 | DR 백업 실패 | 90일 보존 + 압축 + 월별 incremental |
| 사용자 BR-V07 override 남용 | 일중 락 의미 손실 | Slack alert (override 1일 5회↑ 발생 시 IT_OPS 통보) |
| AG Grid Enterprise 라이센스 만료 | 빌드 차단 | 라이센스 키 만료일 모니터 + Renew Q3 |

---

## 10. Phase 4 종료 후 — Phase 5 (PROD) + Phase 6 (운영) 로드맵

| Phase | 기간 | 핵심 |
|---|---|---|
| **Phase 5** PROD cutover | Q3 2026 | Blue → Green 실 절환 + PROD 사용자 인계 |
| **Phase 6** 운영 + carry-over | Q4 2026 ~ | Sprint 7+ (BR-V12·V13 활성 + ML 추천 + 모바일 + Phase 2+ 확장) |

---

## 11. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Phase 4 (STG 베타 5주, 5 phase mile stone) 진입 계획 + PROD cutover 게이트 |
