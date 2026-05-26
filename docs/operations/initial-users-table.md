# 베타 초기 사용자 PIN 발급표 — Sprint 10 EP-AUTH (NFR-SEC-007)

**작성일**: 2026-05-27 | **상태**: 베타 운영 시작 시 IT_OPS 가 사용자별 분배 후 본 문서 폐기 또는 secured archive.

> **⚠️ 보안 주의** — 본 문서는 베타 초기 임시 PIN 발급표. 실 베타 진입 후 각 사용자가 **첫 로그인 즉시 PIN 변경** (Sprint 12 EP-MASTER-UI 의 IT_OPS 사용자 관리 페이지). 본 문서는 git 에 commit 되지 않고 IT_OPS 가 별도 secured channel 로 분배.

---

## 1. 베타 사용자 명단 (V037 시드)

| 사번 | 역할 | 초기 PIN | 직책 (베타 분배 시 IT_OPS 가 매핑) |
|---|---|---|---|
| 00000001 | PLANNER | 0001 | (TBD — 생산계획팀장) |
| 00000002 | PLANNER | 0002 | (TBD — 생산계획팀원 1) |
| 00000003 | PLANNER | 0003 | (TBD — 생산계획팀원 2) |
| 00000004 | STK_USER | 0004 | (TBD — 성형 현장 STK 1) |
| 00000005 | STK_USER | 0005 | (TBD — 성형 현장 STK 2) |
| 00000006 | STK_USER | 0006 | (TBD — 압출 현장 STK) |
| 00000007 | IT_OPS | 0007 | (TBD — IT 운영 담당) |
| 00000008 | READ_ONLY | 0008 | (TBD — 임원 또는 감사) |

---

## 2. 첫 로그인 절차 (사용자 안내)

1. http://schedule.intranet (사내 LAN) 또는 운영자 안내 URL 접속
2. 사번 8자리 + 임시 PIN 4자리 입력 → 로그인
3. **즉시 PIN 변경** — 우측 상단 [사번 (Role)] 영역 → "PIN 변경" (Sprint 12 EP-MASTER-UI 진입 후 활성)
4. 새 PIN — 본인만 아는 4자리 숫자. 다른 사용자 사번과 동일한 PIN 금지 (운영 권고).

---

## 3. 보안 정책 (NFR-SEC-007 v1.5)

| 항목 | 정책 |
|---|---|
| PIN 형식 | 숫자 4자리 (regex `^[0-9]{4}$`) |
| BCrypt strength | 12 (5명 사용자 + 사내 LAN 격리로 응답 ~200ms acceptable) |
| 실패 잠금 | **5회 연속 실패 → 10분 자동 잠금** (locked_until) |
| 잠금 해제 | 10분 자동 / IT_OPS 수동 (Sprint 12 사용자 관리 페이지) |
| JWT 유효기간 | 8시간 (`JwtService.TOKEN_VALIDITY`) |
| JWT secret | env var `JWT_HMAC_SECRET` (PROD 진입 시 필수, DEV default 는 INSECURE) |
| 토큰 만료 후 | 자동 logout → /login redirect (`apiFetch` 401 처리) |

---

## 4. 잠금 해결 (사용자별)

- **잠긴 사용자** — 10분 대기 후 재시도. 또는 IT_OPS 에게 즉시 해제 요청.
- **IT_OPS 수동 해제** (Sprint 12 페이지 활성 전 임시) — IT_OPS 가 PSQL 직접:
  ```sql
  UPDATE app.user_account
  SET failed_attempts = 0, locked_until = NULL
  WHERE employee_id = '00000001';   -- 잠긴 사번
  ```

---

## 5. PIN 분실 처리 (IT_OPS)

- Sprint 12 진입 전 임시 절차 — IT_OPS 가 PSQL:
  ```sql
  -- 새 PIN '1234' 로 reset
  UPDATE app.user_account
  SET pin_hash = crypt('1234', gen_salt('bf', 12)),
      failed_attempts = 0,
      locked_until = NULL
  WHERE employee_id = '00000001';
  ```
- audit_log 에 `actor='system'` 으로 reset 기록 자동 (BR-X02).
- 사용자에게 새 PIN 안내 후 첫 로그인 직후 변경 권고.

---

## 6. 베타 종료 후 처리

- Sprint 12 EP-MASTER-UI 가 활성되면 본 임시 PIN 정책 폐기 — IT_OPS 가 신규 사용자 추가 + PIN 분배 UI 통해 운영.
- 본 문서는 별도 secured archive (예: 1Password 그룹 vault) 로 이관 후 git/repo 에서 삭제.
