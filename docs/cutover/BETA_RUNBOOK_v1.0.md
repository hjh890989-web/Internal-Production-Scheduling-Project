# 베타 Cutover Runbook v1.0

**대상**: 송우산업 사내 베타 운영 진입 단계별 실행 가이드
**작성일**: 2026-05-28 | **버전**: 1.0 | **참조**: Sprint 19 EP-BETA-LAUNCH TK-BETA-6-2

> 시간순 T-1주 → T-1일 → T-1시간 → **T0 cutover** → T+1시간 health check 5단계. 각 단계 완료 시 √ 표시 + 시각 기록.

---

## T-1주 (cutover 7일 전)

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | [Go/No-Go 체크리스트](BETA_GO_NOGO_CHECKLIST_v1.0.md) 11 항목 모두 ✓ | 전체 | ⬜ | |
| 2 | 베타 사용자 8명 사전 안내 (사번 + 초기 PIN 개별 전달) | IT_OPS | ⬜ | |
| 3 | `infrastructure/observability/grafana-dashboard.json` 4 패널 시각 검증 + alerting rule 설정 | IT_OPS | ⬜ | |
| 4 | 사용자 매뉴얼 v1.0 배포 (PDF or markdown link) — 사번 8명에게 전송 | 개발 | ⬜ | |
| 5 | NSSM 설치 + 자동시작 등록 (`install-nssm-services.ps1`) → PC 재부팅 1회 자동 기동 검증 | IT_OPS | ⬜ | |
| 6 | DB 백업 1회 실행 (`pg_basebackup`) + 복구 시뮬 1회 (다른 디렉토리에 restore) | IT_OPS | ⬜ | |

---

## T-1일 (cutover 24h 전)

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | STG 환경에서 V045 cleanup 함수 호출 1회 + 결과 확인 | 개발 | ⬜ | |
| 2 | Frontend tsc + vitest GREEN 재확인 (`cd frontend && npx tsc --noEmit && npx vitest run`) | 개발 | ⬜ | |
| 3 | Backend IT 60+/60+ GREEN 재확인 | 개발 | ⬜ | |
| 4 | 본 PC 시각 검증 — E2E 시나리오 1회 (수주 commit → confirm → MES degraded → Drawer) | 개발 + IT_OPS | ⬜ | |
| 5 | NGINX TLS 인증서 만료일 확인 (30일+ 잔여 권장) | IT_OPS | ⬜ | |
| 6 | Docker Desktop 자동시작 활성 + Postgres/Redis 컨테이너 `--restart=unless-stopped` 설정 | IT_OPS | ⬜ | |

---

## T-1시간 (cutover 60분 전)

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | 베타 사용자 8명 임박 안내 (60분 전 메일/메신저) | IT_OPS | ⬜ | |
| 2 | Backend/Frontend 서비스 중지 (`Stop-Service Scheduling-Backend, Scheduling-Frontend`) | IT_OPS | ⬜ | |
| 3 | DB 마지막 백업 1회 (`pg_basebackup --label="cutover-T-1h"`) | IT_OPS | ⬜ | |
| 4 | V045 PROD 적용 직전 — `flyway_schema_history` 에 v045 row 존재 확인 | 개발 | ⬜ | |

---

## T0 (cutover 시각)

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | **99999-SAMPLE cleanup 실행** — `docker exec scheduling-postgres psql -U app_user -d scheduling -c "SET app.audit_actor='cutover-T0'; SELECT * FROM app.cleanup_99999_samples();"` → deleted_count 기록 | 개발 | ⬜ | |
| 2 | **Backend 시작** — `Start-Service Scheduling-Backend` → `Get-Content backend\logs\nssm-backend-stdout.log -Tail 50` 으로 `Started SchedulingApplication` 확인 | IT_OPS | ⬜ | |
| 3 | **Frontend 시작** — `Start-Service Scheduling-Frontend` → port 5173 listening 확인 | IT_OPS | ⬜ | |
| 4 | **Smoke test** — http://localhost:8080/api/actuator/health → `{"status":"UP"}` + http://localhost:5173 → 로그인 화면 표시 | IT_OPS | ⬜ | |
| 5 | PLANNER1 (00000001 / 0001) 로그인 + 종 아이콘 + 시뮬뷰 진입 1회 검증 | IT_OPS | ⬜ | |

---

## T+1시간 (cutover 60분 후 health check)

| # | 작업 | 책임 | 완료 √ | 시각 |
|:--:|---|---|:--:|---|
| 1 | Grafana 대시보드 — HTTP p95 < 2s + CRITICAL retry rate = 0 + MES degraded = 0 | IT_OPS | ⬜ | |
| 2 | 베타 사용자 첫 로그인 8명 모두 성공 (audit_log INSERT 8건 확인) | 개발 | ⬜ | |
| 3 | 첫 alarm 없음 (Slack/Drawer 신규 메시지 0건) | IT_OPS | ⬜ | |
| 4 | DB connection pool — HikariCP active < 5 / 30 (여유) | IT_OPS | ⬜ | |
| 5 | NSSM 서비스 Status=Running (재시작 fail 없음) | IT_OPS | ⬜ | |

**모두 ✓ → 베타 운영 시작 공지 (사내 메신저)**

---

## 비상 시 롤백 절차

| 트리거 | 절차 |
|---|---|
| Backend 부팅 실패 | `Stop-Service Scheduling-Backend` → 로그 분석 → 코드 수정 또는 직전 git tag 로 revert (`git checkout v18-final-release && cd backend && ./gradlew :app:bootRun`) |
| V045 cleanup 실수 호출 (운영 데이터 삭제 위험) | 즉시 백업에서 restore: `pg_basebackup` 압축 해제 + Postgres data dir 복원 + 컨테이너 재시작 |
| 사용자 로그인 일괄 실패 | `SELECT * FROM app.user_account` 시드 확인 → V037 미적용 시 backend 재시작 (Flyway 자동 적용) |
| Frontend 빈 화면 | F12 console 에러 확인 → Sprint 18 SockJS polyfill 회귀 의심 시 `frontend/index.html` `window.global = window` 확인 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Sprint 19 EP-BETA-LAUNCH cutover 5 단계 (T-1주/T-1일/T-1시간/T0/T+1시간) 시간순 가이드. 비상 시 롤백 4 트리거. 각 작업 책임 + 시각 기록 칸. |
