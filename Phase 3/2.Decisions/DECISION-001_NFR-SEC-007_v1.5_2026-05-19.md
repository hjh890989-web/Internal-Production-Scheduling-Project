# DECISION-001 — NFR-SEC-007 로그인·비밀번호 정책 재정의 (v1.5)

**일자**: 2026-05-19 | **결재**: 사용자 결정 | **상태**: 적용 (commit `65359e6`)
**영향 범위**: SRS v1.5, Keycloak realm, TK-42-6-3, 운영 가이드 2건

---

## 1. 결정 (Decision)

> SRS v1.4 의 NFR-SEC-007 (12자/3종/15분 잠금) 정책을 폐기하고,
> 다음과 같이 재정의한다 (NFR-SEC-007 v1.5, Must 승격):
>
> 1. **Login ID** = 사번 (숫자 8자리). 이메일 로그인 불허.
> 2. **비밀번호** = 숫자 4자리 PIN (Keycloak `regexPattern(^[0-9]{4}$)`).
> 3. **잠금** = 5회 실패 → 10분 자동 해제 (`maxFailureWaitSeconds=600`).
> 4. **emergency 계정 사번 예약 영역**: `99000001~99000003` (PLANNER × 2 + IT_OPS × 1).

---

## 2. 배경 (Context)

- 사용자 ~10명 한정 (사내 운영)
- 사내망 격리 (NFR-SEC-001) — 외부 접근 차단
- 터미널 입력 편의 우선 (P3·P4 페르소나 현장 단말 사용 시나리오)
- v1.4 (12자/3종) 은 외부 노출 시스템 기준 — 본 시스템 컨텍스트 과적합

---

## 3. 보안 영향 분석

### 완화 요인
- 사번 8자리 + PIN 4자리 = 사실상 12자리 비밀번호 효과 (사번 외부 노출 차단 전제)
- 10,000 PIN 조합 × 5회 잠금 = 100시간/시도 brute force 한도 → 사실상 안전
- 사용자 ~10명 named user → 사용자별 행위 추적 가능 (audit)

### 잔여 위험
- **사회공학 공격** — IT 보안 교육 분기 캠페인 (secrets-management.md §PIN 정책)
- **어깨너머 보기** — 블록 입력기 사용 권장
- **emergency PIN 봉인 누설** — IT lead 봉투 봉인 + 사용 시 audit (idp-failover.md)
- **PIN 공유** — 1인 1계정 audit 정책 (사용자별 권한 분리)

---

## 4. 영향 문서 갱신 (commit `65359e6`)

| 파일 | 변경 |
|---|---|
| `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md` | 신규 — REQ-NF-SEC-007 재정의 + 개정 이력 |
| `Phase 2/4.Tasks/Tasks/EP-42/ST-42-6/TK-42-6-3.md` | in-place 갱신 — passwordPolicy + AC + 개정 이력 |
| `infrastructure/keycloak/realm-scheduling-system.json` | passwordPolicy regex + maxFailureWait 600 + loginWithEmail false + 99xxxxxx 사용자 |
| `docs/operations/secrets-management.md` | §3.2 PIN 정책 운영 + 보안 교육 |
| `docs/operations/idp-failover.md` | emergency 사번 + PIN 형식 갱신 |
| `docs/operations/slack_channels.md` | NFR-SEC-007 → NFR-SEC-005 (token rotation 정확성) |

---

## 5. 적용 절차 (DB 데이터 보존)

| 환경 | 절차 |
|---|---|
| DEV | 현재 컨테이너 기존 realm 유지. 다음 fresh boot 또는 Admin Console 수동 적용 |
| STG/PROD | 배포 시 `docker volume rm scheduling_keycloak-db-data` 1회 (IT lead 결재) 후 fresh import |

---

## 6. 검증 (적용 후)

- [ ] 사번 (8자리 숫자) 로그인 성공
- [ ] 이메일 로그인 시도 → HTTP 401
- [ ] 4자리 PIN 외 거부 (`1234` ✓ / `12345` ✗ / `abcd` ✗)
- [ ] 5회 실패 → 계정 잠금 + 10분 후 자동 해제
- [ ] emergency 계정 (99000001/99000002/99000003) PIN 9001/9002/9003 로 로그인 (temporary=true, 첫 로그인 시 변경)

---

## 7. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | Claude Code (사용자 결정 기록) | 초안 — DECISION-001 작성. 적용 commit `65359e6` (2026-05-19) |
