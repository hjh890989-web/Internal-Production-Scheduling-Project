# 베타 Go/No-Go 체크리스트 v1.0

**대상**: 송우산업 사내 베타 cutover 진입 전 11 항목 검증
**작성일**: 2026-05-28 | **버전**: 1.0 | **참조**: Sprint 19 EP-BETA-LAUNCH TK-BETA-6-1

> 본 체크리스트는 PROD 운영 진입 직전 11/11 ✓ 후에만 cutover. 1 항목이라도 실패 시 No-Go 결정 + 미해결 항목 보완 후 재검증.

---

## 1. 검증 항목 (11)

| # | 항목 | 검증 명령 / 절차 | 합격 기준 |
|:--:|---|---|---|
| **1** | **Backend IT 회귀 GREEN** | `cd backend && ./gradlew :app:test --tests "com.scheduling.integration.*"` | 60+/60+ PASSED (Sprint 16/17/18/19 통합) |
| **2** | **BetaE2EIntegrationIT 1/1 GREEN** | `./gradlew :app:test --tests "com.scheduling.integration.BetaE2EIntegrationIT"` | 4/4 cases PASSED (E2E 단일 시나리오) |
| **3** | **Frontend tsc + vitest GREEN** | `cd frontend && npx tsc --noEmit && npx vitest run` | tsc 0 errors + vitest 82+/82+ |
| **4** | **Grafana dashboard 4+ 패널** | http://localhost:3000 → 4 dashboard 시각 확인 | scheduling-overview / application-overview / notify-sprint18 / logs-overview |
| **5** | **사용자 매뉴얼 v1.0 배포** | `docs/manual/USER_MANUAL_v1.0.md` 파일 존재 + 4 role 섹션 포함 | 7 섹션 (공통/PLANNER/STK_USER/IT_OPS/READ_ONLY/PIN/장애) |
| **6** | **NSSM 자동시작 활성** | `Get-Service Scheduling-Backend, Scheduling-Frontend` → Status=Running, StartType=Automatic | PC 재부팅 1회 후 자동 기동 확인 (port 8080 + 5173) |
| **7** | **V045 cleanup STG 검증** | STG DB 에서 `SELECT * FROM app.cleanup_99999_samples();` 호출 | 함수 정의 + 호출 가능 (PROD 적용 전 STG 에서 검증) |
| **8** | **베타 사용자 8명 시드** | `SELECT employee_id, role FROM app.user_account ORDER BY employee_id;` | 8 row (00000001~8) + role 정합 (PLANNER 3 + STK 3 + IT_OPS 1 + READ 1) |
| **9** | **PIN 0001~0008 로그인** | 각 사번 + PIN 4자리로 8회 로그인 시도 (또는 API smoke) | 모두 200 + JWT 발급 + 첫 로그인 후 PIN 변경 권고 안내 |
| **10** | **Slack/Kakao config flag default false** | `application.yml` 확인: `scheduling.notification.slack.enabled=false` + `scheduling.notification.kakao.enabled=false` | 둘 다 false (Phase 4+ 실 webhook URL 발급 전 안전) |
| **11** | **NGINX TLS + DB 백업 1회** | (1) NGINX `infrastructure/nginx/nginx.conf` 인증서 경로 유효 + 갱신일 확인. (2) `pg_basebackup` 1회 수동 실행 + 복구 절차 검증 | TLS 1.2/1.3 활성 + 백업 파일 압축 < 100MB + restore 시뮬 성공 |

---

## 2. 검증 결과 (cutover 직전 1회 작성)

| # | 항목 | 결과 | 검증자 | 검증일시 | 비고 |
|:--:|---|:--:|---|---|---|
| 1 | Backend IT 회귀 | ⬜ Pass / ⬜ Fail | | | |
| 2 | BetaE2EIntegrationIT | ⬜ Pass / ⬜ Fail | | | |
| 3 | Frontend tsc + vitest | ⬜ Pass / ⬜ Fail | | | |
| 4 | Grafana 4+ 패널 | ⬜ Pass / ⬜ Fail | | | |
| 5 | 사용자 매뉴얼 v1.0 | ⬜ Pass / ⬜ Fail | | | |
| 6 | NSSM 자동시작 | ⬜ Pass / ⬜ Fail | | | |
| 7 | V045 cleanup STG | ⬜ Pass / ⬜ Fail | | | |
| 8 | 베타 사용자 8명 시드 | ⬜ Pass / ⬜ Fail | | | |
| 9 | PIN 0001~0008 로그인 | ⬜ Pass / ⬜ Fail | | | |
| 10 | Slack/Kakao flag false | ⬜ Pass / ⬜ Fail | | | |
| 11 | NGINX TLS + DB 백업 | ⬜ Pass / ⬜ Fail | | | |
| **합계** | | **? / 11** | | | **11/11 → Go / 그 외 → No-Go** |

---

## 3. No-Go 시 대응

| 실패 항목 | 후속 조치 |
|---|---|
| 1, 2, 3 (IT/test) | 실패 IT log 분석 → bug fix commit → 재검증 (반복) |
| 4 (Grafana) | provisioning yml 확인 → Grafana 재시작 → datasource 검증 |
| 5 (매뉴얼) | 매뉴얼 작성 / 갱신 후 v1.1 발행 |
| 6 (NSSM) | install-nssm-services.ps1 관리자 권한 재실행 → services.msc 수동 확인 |
| 7 (V045) | Flyway 적용 확인 (`SELECT * FROM flyway_schema_history WHERE version='045'`) — 미적용 시 backend 재기동 |
| 8 (사용자) | V037 seed 적용 확인 → 미적용 시 backend 재기동 |
| 9 (로그인) | BCrypt strength 12 비교 확인 → PIN 재해시 (`crypt('0001', gen_salt('bf', 12))`) |
| 10 (flag) | application.yml 직접 확인 + backend 재시작 |
| 11 (TLS/백업) | 사내 IT 협의 — 인증서 갱신 일정 + 백업 매뉴얼 작성 |

---

## 4. 책임 분담

| 영역 | 검증 책임 |
|---|---|
| 1, 2, 3 (코드 IT/test) | 개발 담당 (Claude Code 협업) |
| 4 (Grafana) | IT_OPS |
| 5 (매뉴얼) | 개발 + IT_OPS 공동 검토 |
| 6 (NSSM), 11 (TLS/백업) | IT_OPS |
| 7 (cleanup), 8 (사용자), 9 (로그인), 10 (flag) | 개발 |

---

## 5. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Sprint 19 EP-BETA-LAUNCH 진입 전 11 항목 체크리스트. 검증/대응/책임 분담 포함. cutover 직전 1회 채워 보관. |
