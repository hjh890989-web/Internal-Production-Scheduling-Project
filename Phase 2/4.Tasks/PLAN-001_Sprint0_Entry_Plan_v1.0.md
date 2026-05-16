# Sprint 0 (Stage 0) 통합 진입 계획 v1.0

**작성일**: 2026-05-16 | **버전**: 1.0 | **상태**: Phase 3 진입 권고안

> **참조**: TASK-001_WBS_v1.2 §3 (Sprint 0 정의) + 4차 감사 결과 (NO-GO → GO) + Phase 2 종료 465 파일·253 SP

---

## 1. 목적

Phase 3 첫 2주 (Sprint 0) 에 **7 Epic 인프라 통합 셋업**. WBS v1.2 §3 정의는 EP-00·99 만이었으나, 4차 감사에서 **EP-30·31·32·33·34** 신규 추가 (P1 risk 해소). 본 문서는 7 Epic의 2주 통합 일정·의존성·병렬화·DoD 정의.

---

## 2. Sprint 0 통합 SP·PD

| Epic | 제목 | SP | PD | Sprint 0 비중 |
|---|---|:--:|:--:|:--:|
| **EP-00** | Docker Compose + Spring Modulith + React 골격 | 8 | 5.6 | 100% (S0) |
| **EP-99** | 마스터 데이터 정비 (47품번 검증) | 5 | 3.5 | 100% (S0) |
| **EP-30** | Keycloak 24 + SAML/OIDC + RBAC | 8 | 5.6 | 100% (S0+S1 분산) |
| **EP-31** | Prometheus + Loki + Grafana | 5 | 3.5 | 100% (S0) |
| **EP-32** | Jenkins + Harbor + SonarQube + Trivy | 5 | 3.5 | 100% (S0) |
| **EP-33** | Docker Compose STG/PROD + pg_basebackup | 5 | 3.5 | 80% (일부 S5 분기 드릴) |
| **EP-34** | BR-X04 KST 통일 (ST-34-3만 S0) | 1 | 0.7 | 20% (BR-X05·X06은 S2~S4) |
| **합계** | | **37 SP** | **~26 PD** | |

**인력 가정**: 3 dev × 2주 (10 영업일) = 30 PD 가용 → **37 SP 부담스러우나 가능** (parallel 활용 시).

> ⚠️ **인력 부족 시나리오**: 2 dev × 2주 = 20 PD → **17 SP 초과**. EP-30 ST-30-2 (RBAC)를 S1 carry-over하면 S0 = 29 SP / 20 PD = 가능.

---

## 3. 의존성 그래프

```
                    ┌──────────────┐
                    │ EP-00 ⭐⭐  │ (Docker Compose + Modulith + React)
                    │ Foundation  │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┬─────────────┬──────────────┐
              ▼            ▼            ▼             ▼              ▼
        ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ EP-30   │  │ EP-31   │  │ EP-32    │  │ EP-33    │  │ EP-99    │
        │ Keycloak│  │ Prom+Loki│  │ Jenkins  │  │ STG/PROD │  │ Master   │
        │ ⭐⭐   │  │ ⭐⭐    │  │ ⭐⭐    │  │ ⭐⭐    │  │ 검증     │
        └────┬────┘  └─────┬───┘  └─────┬────┘  └─────┬────┘  └──────────┘
             │             │            │             │
             │   ┌─────────┘            │             │
             │   │                      │             │
             ▼   ▼                      │             │
        ┌─────────────┐                 │             │
        │ EP-34 KST   │◄────────────────┘             │
        │ (ST-34-3만) │                              │
        └─────────────┘                              │
                                                     ▼
                                          ┌──────────────────────┐
                                          │ Sprint 0 DoD 통합:  │
                                          │ 첫 빌드 + STG 부팅 + │
                                          │ Grafana healthy + ... │
                                          └──────────────────────┘
```

**의존성 요약**:
- **EP-00 = Foundation** — 모든 Epic 선행
- **EP-30·31·32·33 = 병렬 가능** (EP-00 완료 후)
- **EP-34 ST-34-3 (KST) = EP-00 완료 후 가능** — Spring·DB·Frontend 설정
- **EP-99 = 완전 독립** — Phase 1 마스터 검증, 다른 Epic 무관

---

## 4. 10 영업일 일정 (3 dev 가정)

| 일 | Dev A (Backend Lead) | Dev B (DevOps Lead) | Dev C (Frontend/QA) |
|:---:|---|---|---|
| **D1** | TK-00-2-1 Gradle 멀티모듈 | TK-00-1-1·2·3 Docker Compose (PG·Redis·NGINX) | TK-00-3-1 Vite + TS |
| **D2** | TK-00-2-2 Spring Modulith 모듈 경계 | TK-00-1-4 Compose v2 통합 검증 + TK-30-1-1 Keycloak 컨테이너 | TK-00-3-2 Ant Design + 한국어 i18n |
| **D3** | TK-00-2-3 ArchUnit + TK-34-3-1 KST | TK-30-1-2 SAML/OIDC 페더레이션 | TK-00-3-3 라우팅 + Zustand |
| **D4** | TK-34-3-2 ArchUnit KST + TK-30-2-1·2 RBAC | TK-30-1-3 Local fallback + TK-31-1-1 Spring Actuator | TK-99-1-1 K/L열 47품번 검증 |
| **D5** | **첫 빌드 시도** + TK-30-2-3 403/401 | TK-31-1-2 Prometheus scrape + TK-31-1-3 Grafana 17 KPI skeleton | TK-99-1-2 28422-08HA0 등 룰 cross-check |
| **D6** | TK-32-1-1 Jenkinsfile 템플릿 | TK-31-2-1 Loki + promtail | TK-99-1-3·2-1·2·3 마스터 무결성 SQL |
| **D7** | TK-32-1-2 build → SonarQube → Harbor | TK-31-2-2 Grafana datasource + TK-31-2-3 AlertManager Slack | (QA 모드) E2E 시뮬 셋업 |
| **D8** | TK-32-1-3 NGINX 무중단 + TK-32-2-1 Trivy | TK-33-1-1 STG 환경 + TK-33-1-3 secrets | 통합 테스트 |
| **D9** | TK-32-2-2 SonarQube quality gate | TK-33-1-2 PROD blue/green + TK-33-2-1 pg_basebackup | 첫 STG 배포 검증 |
| **D10** | TK-32-2-3 빌드 실패 알림 | TK-33-2-2 WAL archiving | **Sprint 0 DoD 검증 + Review 데모** |

---

## 5. Critical Path ⭐⭐⭐

**가장 위험한 chain (지연 시 전체 Sprint 0 차단)**:

```
TK-00-1-4 (Compose 통합 검증) →
TK-30-1-1 (Keycloak 컨테이너) →
TK-30-1-2 (SAML 페더레이션) →
TK-32-1-2 (Jenkins → SonarQube → Harbor) →
TK-33-1-2 (Blue/Green 배포) →
첫 STG 배포 healthy
```

**Critical Path PD: ~7 PD** (D1~D9에 걸침)

**완화 방안**:
- Keycloak 페더레이션 (TK-30-1-2) — 사내 IdP 확정 안 되면 Local fallback만 우선 사용 (TK-30-1-3 우선)
- Jenkinsfile (TK-32-1-1) — 첫 빌드 단순화 (test → image build → push만, SonarQube 후속)

---

## 6. 병렬화 매트릭스

| 일자 | 병렬 작업 수 | 위험 |
|:---:|:---:|---|
| D1~D2 | 3 (전 dev 독립) | 낮음 — Foundation 분리 |
| D3~D4 | 3 (Modulith·Keycloak·UI 병렬) | 중간 — 모듈 경계 결정이 다른 dev 영향 |
| D5 | **첫 빌드 시도 = 통합 risk** | 높음 — 모든 dev 합동 디버깅 가능성 |
| D6~D8 | 3 (CI/CD·관측·STG 병렬) | 낮음 — 독립 인프라 |
| D9 | **STG 첫 배포 = 통합 risk** | 높음 — Blue/Green 환경 검증 |
| D10 | DoD 검증 (전 dev 합동) | 낮음 |

---

## 7. Sprint 0 DoD 통합 (검수 체크리스트)

| # | 항목 | 출처 Epic | 검증 |
|:--:|---|:---:|---|
| 1 | `docker compose up` → 5 컨테이너 healthy (DEV 환경) | EP-00 | health check |
| 2 | Spring Modulith ArchUnit PASS (모듈 경계 위반 0) | EP-00 | CI 자동 |
| 3 | 마스터 K/L열·B열 무결성 100% (47품번) | EP-99 | SQL 검증 |
| 4 | Keycloak 24 booting + realm `scheduling-system` import | EP-30 | `/health/ready` |
| 5 | Spring Security JWT 검증 + 4 role 매핑 | EP-30 | 단위 테스트 |
| 6 | Prometheus scrape 정상 (5 target) | EP-31 | `/api/v1/targets` |
| 7 | Loki 로그 수집 + Grafana datasource OK | EP-31 | Grafana Explore |
| 8 | AlertManager → Slack webhook (test alert) | EP-31 | Slack 메시지 도달 |
| 9 | Jenkins 첫 빌드 PASS (build → test → image push) | EP-32 | Jenkins UI green |
| 10 | SonarQube quality gate 통과 (커버리지 ≥ 50% 초기) | EP-32 | Sonar dashboard |
| 11 | Trivy 이미지 스캔 — Critical 취약점 0건 | EP-32 | Trivy report |
| 12 | STG 환경 booting + NGINX 응답 | EP-33 | curl 200 OK |
| 13 | pg_basebackup 첫 백업 성공 + S3·NAS 업로드 | EP-33 | S3 ls |
| 14 | 모든 timestamp KST (Spring·DB·Frontend) | EP-34 | ArchUnit + 단위 테스트 |
| 15 | **Sprint Review 데모**: 첫 STG 배포 라이브 시연 | 통합 | 팀 합의 |

---

## 8. Risk·완화 매트릭스

| Risk | 영향 | 확률 | 완화 |
|---|:---:|:---:|---|
| 사내 IdP 확정 지연 (EP-30 ST-30-1-2) | 중 | 중 | Local fallback만 우선 진행 (TK-30-1-3) |
| Modulith 모듈 경계 잘못 분리 (EP-00 ST-00-2-2) | 높음 | 낮음 | SAD §5.1 그대로 적용 — 7 모듈 (order·vc·ex·master·audit·notify·common) |
| 잉여 서버 사양 미달 (EP-46 ST-46-1) | 중 | 낮음 | Phase 0 IT 확인 — 사전 발견 시 신규 도입 견적 |
| Trivy Critical 취약점 발견 (EP-32 ST-32-2-1) | 높음 | 중 | Base image (Eclipse Temurin) 검증 — 발견 시 대체 |
| 47품번 마스터 K/L열 데이터 오류 (EP-99 ST-99-1) | 높음 | 중 | Phase 1 분석 결과 가정 — 오류 시 운영팀 즉시 confirm·수정 |

---

## 9. Sprint 1 진입 전 검수 (D10 종료 시점)

- [ ] Sprint 0 DoD 15 항목 모두 PASS
- [ ] STG 환경 정상 booting + health check
- [ ] Jenkins 자동 빌드 + 배포 chain 동작
- [ ] 베타 candidate user 1명에게 Keycloak 로그인 검증
- [ ] **Sprint 1 시작 가능** (EP-01 수주 통합 Parser)

---

## 10. 사용자 의사결정 사항 (사전 합의 필요)

1. **인력 배정**: 2 dev or 3 dev 확정 (회의 STK-01·STK-06)
2. **사내 IdP**: AD FS / Microsoft Entra ID / Okta — Phase 0 IT 부서 확정
3. **STG·PROD 서버**: 잉여 서버 사양 검증 결과 (TK-46-1-1)
4. **Slack webhook URL**: #scheduling-alerts 채널 생성 + URL 발급
5. **Sentry / Datadog**: SaaS 사용 가능 여부 (보안팀 검토)
6. **Harbor**: 사내 사용 가능 — IT 협조 필요
7. **S3 / NAS 백업**: AWS 비용 vs NAS 활용 결정

---

## 11. 다음 단계

1. **본 계획 v1.0 사용자 검토** — STK-01·STK-06·STK-08
2. **인력 확정** + **사내 IdP 확정** + **Slack webhook 생성**
3. **Sprint 0 D1 킥오프 미팅** — 모든 dev + PM 참여
4. **Daily standup 9시 KST** 시작
5. **D10 종료 시점 Sprint Review** + **Sprint 1 진입 결정**

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-16 | (작성자) | 초안 — Phase 2 종료 후 Phase 3 진입 통합 계획 (4차 감사 GO 판정 기반) |
