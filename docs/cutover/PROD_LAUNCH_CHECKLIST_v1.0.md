# PROD 정식 운영 Cutover Checklist v1.0

**대상**: 송우산업 사내 공정 스케줄링 시스템 베타 → 정식 운영 진입 단계별 실행 가이드
**작성일**: 2026-06-01 | **버전**: 1.0 | **참조**: Sprint 25 EP-PROD-LAUNCH ST-PROD-4 / [BETA_RUNBOOK_v1.0.md](BETA_RUNBOOK_v1.0.md) 확장

> 베타 Runbook 패턴 (T-1주 6항 + T-1일 6항 + T0 5항 + T+1시간 5항) 을 그대로 계승 + PROD 환경 신규 항목 확장. 각 단계 완료 시 √ 표시 + 시각 기록 (KST, BR-X04).

---

## T-1주 (PROD cutover 7일 전) — 9 항

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | [BETA_GO_NOGO_CHECKLIST](BETA_GO_NOGO_CHECKLIST_v1.0.md) 11 항목 모두 ✓ (베타 마감 기준) | 전체 | ⬜ | |
| 2 | PROD 사용자 30명 사전 안내 (사번 + 초기 PIN 개별 메일) — 베타 8 + 신규 22 | IT_OPS | ⬜ | |
| 3 | Grafana 4 패널 시각 검증 + alerting rule (MES degraded > 0 / HTTP 5xx / CPU > 80%) | IT_OPS | ⬜ | |
| 4 | 사용자 매뉴얼 v1.5 PROD 배포 (PDF link 30명 메일) | 개발 | ⬜ | |
| 5 | NSSM 자동시작 등록 + PC 재부팅 1회 자동 기동 검증 | IT_OPS | ⬜ | |
| 6 | DB 백업 1회 (`pg_basebackup`) + 복구 시뮬 1회 (다른 디렉토리) | IT_OPS | ⬜ | |
| 7 | **[신규] KPI 30일 누적 결과 보고서** — HTTP p95 / MES degraded count / audit INSERT 율 / 에러율 (베타 30일 누적) | 개발 + IT_OPS | ⬜ | |
| 8 | **[신규] k6 부하 30명 시나리오 결과** — 1500 row 조회 p95 < 800ms / 동시 30 사용자 5분 sustained / 실패율 0% | 개발 | ⬜ | |
| 9 | **[신규] Blue/Green dry-run 검증** — NGINX upstream toggle (`schedule_blue` ↔ `schedule_green`) 무중단 전환 1회 (베타 환경에서 시뮬) | IT_OPS | ⬜ | |

---

## T-1일 (PROD cutover 24h 전) — 6 항

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | STG 환경에서 V045 cleanup 함수 호출 1회 + 결과 확인 | 개발 | ⬜ | |
| 2 | Frontend tsc + vitest GREEN 재확인 (`cd frontend; npx tsc --noEmit; npx vitest run`) | 개발 | ⬜ | |
| 3 | Backend `./gradlew verifyAll` 전체 GREEN 재확인 (~13분, Sprint 마감 기준) | 개발 | ⬜ | |
| 4 | E2E 시나리오 1회 (수주 commit → confirm → MES degraded → Drawer → audit log INSERT 확인) | 개발 + IT_OPS | ⬜ | |
| 5 | NGINX TLS 인증서 만료일 확인 (30일+ 잔여 권장) | IT_OPS | ⬜ | |
| 6 | Docker Desktop 자동시작 + Postgres/Redis `--restart=unless-stopped` 설정 재확인 | IT_OPS | ⬜ | |

---

## T0 (PROD cutover 시각) — 5 항

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | **99999-SAMPLE cleanup 실행** — `docker exec scheduling-postgres psql -U app_user -d scheduling -c "SET app.audit_actor='prod-cutover-T0'; SELECT * FROM app.cleanup_99999_samples();"` → deleted_count 기록 | 개발 | ⬜ | |
| 2 | **Backend 시작** — `Start-Service Scheduling-Backend` → `Get-Content backend\logs\nssm-backend-stdout.log -Tail 50` 으로 `Started SchedulingApplication` 확인 | IT_OPS | ⬜ | |
| 3 | **Frontend 시작** — `Start-Service Scheduling-Frontend` → port 5173 listening 확인 | IT_OPS | ⬜ | |
| 4 | **Smoke test** — http://schedule.intranet/api/actuator/health → `{"status":"UP"}` + 메인 URL → 로그인 화면 표시 | IT_OPS | ⬜ | |
| 5 | PLANNER1 (00000001) + 신규 STK_USER1 + IT_OPS 3명 로그인 + 종 아이콘 + 시뮬뷰 진입 1회 검증 | IT_OPS | ⬜ | |

---

## T+1시간 (PROD cutover 60분 후 health check) — 5 항

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | Grafana — HTTP p95 < 2s + CRITICAL retry rate = 0 + MES degraded = 0 + CPU < 50% | IT_OPS | ⬜ | |
| 2 | PROD 사용자 첫 로그인 30명 중 80%+ 성공 (audit_log INSERT 24건+ 확인) | 개발 | ⬜ | |
| 3 | 첫 alarm 없음 (Slack/Drawer 신규 CRITICAL 메시지 0건) | IT_OPS | ⬜ | |
| 4 | DB connection pool — HikariCP active < 10 / 30 (PROD 30명 동시 기준) | IT_OPS | ⬜ | |
| 5 | NSSM 서비스 Status=Running (재시작 fail 없음) + 메모리 leak 미발생 (RSS 안정) | IT_OPS | ⬜ | |

**모두 ✓ → PROD 운영 시작 공지 발송 ([PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0.md](PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0.md) §1 메일 + §2 사내 IM)**

---

## 비상 시 롤백 절차 (베타 Runbook 계승, 4 트리거 유지)

| 트리거 | 절차 |
|---|---|
| Backend 부팅 실패 | `Stop-Service Scheduling-Backend` → 로그 분석 → 코드 수정 또는 직전 git tag 로 revert (Blue/Green 환경 시 NGINX upstream toggle 로 즉시 전환) |
| V045 cleanup 실수 호출 (운영 데이터 삭제 위험) | 즉시 백업에서 restore: `pg_basebackup` 압축 해제 + Postgres data dir 복원 + 컨테이너 재시작 |
| 사용자 로그인 일괄 실패 | `SELECT * FROM app.user_account` 시드 확인 → V037 미적용 시 backend 재시작 (Flyway 자동 적용). PROD 30명 → 베타 8명 fallback 가능 |
| Frontend 빈 화면 | F12 console 에러 확인 → SockJS polyfill 회귀 의심 시 `frontend/index.html` `window.global = window` 확인 |

---

## 베타 Runbook 대비 변경점 요약

| 항목 | 베타 Runbook v1.0 | PROD Checklist v1.0 |
|---|---|---|
| 사용자 수 | 8명 | 30명 (베타 8 + 신규 22) |
| T-1주 항목 수 | 6 | **9** (+#7 KPI 30일 + #8 k6 30명 + #9 Blue/Green dry-run) |
| 접속 URL | http://localhost:8080 / 5173 | http://schedule.intranet (NGINX TLS) |
| T+1시간 audit INSERT | 8건 (전수) | 24건+ (80% 임계) |
| HikariCP 임계 | active < 5 / 30 | active < 10 / 30 |
| T-1시간 단계 | 4 항 (베타) | **생략 가능** (베타 운영 중 시스템 가동 상태에서 무중단 cutover 시) — 단, 재기동 필요 시 베타 동일 4 항 수행 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-06-01 | Claude Code | 초안 (data-free) — Sprint 25 S25-A ST-PROD-4. BETA_RUNBOOK v1.0 패턴 (T-1주 6 + T-1일 6 + T0 5 + T+1시간 5) 계승 + T-1주 #7~9 신규 (KPI 30일 / k6 30명 / Blue/Green dry-run). 비상 롤백 4 트리거 유지. |
