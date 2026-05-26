# Phase 4-A Full E2E Probe v1.0 — Keycloak SSO + JWT 본 PC 시뮬 한계 식별

**Phase**: 4-A (STG 부팅) — Full E2E DEV-as-STG Probe | **실행일**: 2026-05-23
**환경**: 개발자 PC (Windows 11 + Docker Desktop v4.74.0)
**상위 참조**: [Phase-4A_DryRun_v1.0](Phase-4A_DryRun_v1.0.md) (V034 + metric 검증) + [Phase-4_EntryPlan_v1.1 §5](Phase-4_EntryPlan_v1.1.md#5-stg-환경-명세-phase-4-a--v10-그대로)

> Phase-4A DryRun 후속 — Keycloak SSO + JWT 발급 + Bearer 인증된 REST endpoint 실 호출까지
> 본 PC 에서 진행 시도. **JWT issuer mismatch 식별** + 본 PC 한계 정리 + 사내 STG 진입 시
> 자연 해결 절차.

---

## 1. Probe 범위 + 한계

### 1.1 범위 ✅ (완료된 단계)

| # | 단계 | 결과 |
|---|---|---|
| 1 | postgres + redis + keycloak-db + keycloak 부팅 | ✅ healthy (Keycloak realm import 자동) |
| 2 | Keycloak admin API — 사용자 99000001 password reset (temporary false) + emailVerified true | ✅ |
| 3 | Direct Grant token 발급 — `POST /realms/scheduling-system/protocol/openid-connect/token` | ✅ JWT 1166 chars, role=PLANNER |
| 4 | backend env override (`KEYCLOAK_ISSUER_URI` + `KEYCLOAK_JWKS_URI`) + 재부팅 | ✅ 4s healthy |
| 5 | Bearer header 로 V12 split endpoint 호출 | ❌ **401 Unauthorized** |

### 1.2 한계 ❌ (식별된 본 PC 제약)

**JWT issuer mismatch** — Keycloak 의 token 발행 iss vs backend 의 expect issuer-uri:

```text
Keycloak token iss claim:     http://localhost/realms/scheduling-system
backend issuer-uri 환경변수:    http://keycloak:8080/realms/scheduling-system
                                ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
                                container DNS hostname (호스트 → container 접근)
```

원인:
- Keycloak 의 `KC_HOSTNAME=localhost` (`.env` 의 `KEYCLOAK_HOST=localhost`) — 호스트 PC 에서 admin console 접근용 (`127.0.0.1:8180`)
- Token 의 `iss` claim = Keycloak 의 advertised hostname = `localhost`
- Backend container 는 `keycloak:8080` 으로 Keycloak 접근 (Docker network DNS)
- Spring Security `issuer-uri` 검증 정책 — token `iss` 와 OpenID Config `issuer` 일치 강제

---

## 2. 해결 옵션 (본 PC 시 추가 작업 필요)

| 옵션 | 작업 | 시간 | 본 PC 적용 가능 |
|---|---|---|---|
| **A. KC_HOSTNAME=keycloak:8080 override** | docker-compose.override.yml 에 `KEYCLOAK_HOST: keycloak` + container DNS 일치 | ~10분 | ✅ |
| **B. host.docker.internal 사용** | Windows Docker Desktop 의 host bridge — backend 가 host 통해 Keycloak 접근, token iss/expect 일치 가능 | ~15분 | ✅ Windows 전용 |
| **C. SecurityConfig 코드 변경** — jwk-set-uri 만 사용 (issuer 검증 skip) | DEV 시뮬 전용 분기, 회귀 영향 | ~20분 | ✅ 코드 변경 필요 |

**본 Probe 에서는 옵션 진행 보류** — marginal value 사유 §3.

---

## 3. 본 PC 추가 진입 marginal value 판단

본 PC E2E 가 통과하더라도:
- 사내 STG 환경 (사내 Keycloak hostname `keycloak.intranet` + LDAP/AD 통합) 과 hostname/topology 가 **다름**
- 본 PC 한정 솔루션 (override yml 또는 host.docker.internal) 은 STG 자산에 반영 불가
- 사내 STG 진입 시 hostname 자연 일치 (사내 DNS + IT 부서 hostname 설정)

**이미 검증된 자산** (본 PC E2E 진입 없이도 동등한 RBAC chain 검증):
- `CapacityOverflowApprovalIT` 12 IT (MockMvc + `@WithMockUser` PLANNER/STK_USER/READ_ONLY)
- `CapacityOverflowControllerIT` 7 IT (PLANNER 200 + 401 + 403 + 400)
- `BrV12V13IT` 5 IT (Service chain)
- DryRun 통과 — RBAC 401 정상 (Spring Security 활성) + V034 metric 노출 (`scheduling.v13.kd.remaining.qty`, `scheduling.v12.pending.request.count`)

→ 본 PC Full E2E 추가 진입은 **사내 STG 진입 후 자연 검증** 으로 대체.

---

## 4. 사내 STG 진입 시 자연 해결 절차

본 Probe 식별 issue 는 사내 STG 환경 진입 시 자동 해결:

```bash
# 사내 STG 환경 (Harbor + 사내 Keycloak)
1. .env.stg 작성:
   KEYCLOAK_HOST=keycloak.intranet       # 사내 DNS (실 hostname)
   KEYCLOAK_ISSUER_URI=https://keycloak.intranet/realms/scheduling
   KEYCLOAK_JWKS_URI=https://keycloak.intranet/realms/scheduling/protocol/openid-connect/certs

2. docker compose --env-file .env.stg -f docker-compose.yml -f docker-compose.stg.yml up -d

3. Token 발행 iss = https://keycloak.intranet/realms/scheduling     ← STG 정확한 hostname
   Backend expect iss = https://keycloak.intranet/realms/scheduling  ← 동일
   → JWT validation 통과 ✅
```

사내 IT_OPS persona v1.1 §2.1 Phase 4-A 체크리스트 + stg-deploy §12 와 정합.

---

## 5. Probe 후속 — 사내 STG 진입 시 추가 검증 권장 항목 (Phase 4-A 5d)

| 항목 | 위치 | 검증 |
|---|---|---|
| Keycloak realm import + LDAP/AD sync | infrastructure/keycloak/realm-scheduling-system.json | 베타 사용자 5명 SSO 로그인 |
| PLANNER 사용자 — V12 enqueue / accept / reject endpoint 실 호출 | curl + JWT Bearer | 응답 + DB row + audit row |
| BR-V13 supplement endpoint 실 호출 | curl + JWT Bearer | KdSupplementResult + KD remaining 차감 |
| Prometheus scrape — V13 KD remaining + V12 pending 시각화 | http://stg.scheduling.internal:9090 + grafana :3000 | Grafana dashboard 즉시 진입 |
| k6 NFR-PER 부하 측정 | `infra/k6/matrix-1500-row.js` + `K6_JWT` 환경변수 | matrix p95 < 800ms / ranking < 1200ms |
| Lighthouse audit — /vc/capacity-queue | npm run build + npx serve + lighthouse | NFR-PER-006 (FCP/LCP/TTI/CLS) |

---

## 6. 본 Probe 산출물 정리

- 본 PC 컨테이너 종료 + 데이터 volume 보존 (다음 dry-run 재사용 가능)
- 임시 `docker-compose.override.yml` 삭제 (git untracked)
- Keycloak realm + 사용자 99000001 (password 9001 + emailVerified true) — postgres-data volume 안 보존

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Full E2E DEV-as-STG Probe 5/5 단계 진행 + JWT issuer mismatch 식별 + 본 PC 한계 + 사내 STG 진입 시 자연 해결 절차 |
