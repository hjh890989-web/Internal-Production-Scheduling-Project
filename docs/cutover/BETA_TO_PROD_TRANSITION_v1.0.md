# 베타 → 정식 (PROD) 전환 안내 v1.0

**대상**: 송우산업 사내 공정 스케줄링 시스템 베타 사용자 8명 + 신규 PROD 사용자 22명
**작성일**: 2026-06-01 | **버전**: 1.0 | **참조**: Sprint 25 S25-A ST-PROD-4 / SRS v1.5 NFR-SEC-007 / USER_MANUAL v1.5

> 본 문서는 베타 운영 1개월 → 정식 운영 전환 시 사용자 계정·PIN·일정·매뉴얼 변동사항을 단일 출처로 정리. 실 발송은 S25-B Go/No-Go 결정 후.

---

## 1. 베타 사용자 8명 — 사번 유지 + PIN 강제 재설정

### 1.1 대상

| 사번 | 권한 (role) | 베타 운영 사용 빈도 (placeholder) |
|---|---|---|
| 00000001 | PLANNER | (KPI 보고서 참조) |
| 00000002 | PLANNER | (KPI 보고서 참조) |
| 00000003 | PLANNER | (KPI 보고서 참조) |
| 00000004 | STK_USER | (KPI 보고서 참조) |
| 00000005 | STK_USER | (KPI 보고서 참조) |
| 00000006 | IT_OPS | (KPI 보고서 참조) |
| 00000007 | IT_OPS | (KPI 보고서 참조) |
| 00000008 | READ_ONLY | (KPI 보고서 참조) |

### 1.2 PIN 재설정 정책 (NFR-SEC-007 준수)

- **DB 조작**: `UPDATE app.user_account SET last_pin_change_at = NOW() - INTERVAL '31 days' WHERE employee_id IN ('00000001', ..., '00000008');`
- **효과**: 90일 정책에 의해 첫 로그인 시 PIN 변경 강제 화면 자동 표시
- **사번 변경 없음**: 기존 사번 그대로 유지 → 사용자 친화 (베타 1개월 학습 효과 보존)
- **이전 PIN 무효화**: V040 `pin_history` 테이블 last 5 비교는 그대로 작동 — 베타 기간 사용 PIN 4건은 재사용 차단
- **audit 기록**: `pin_changed` 이벤트 BR-X02 자동 INSERT (8건)

### 1.3 변경 안내 (베타 사용자 8명 개별 메일)

```
{RECIPIENT_NAME} 님,

베타 1개월 운영에 참여해 주셔서 감사합니다.
{LAUNCH_DATE} 부터 정식 운영으로 전환됩니다.

■ 변경사항
  - 사번: {EMPLOYEE_ID} (변경 없음, 유지)
  - PIN: 보안 정책상 첫 로그인 시 강제 재설정
  - URL: {ACCESS_URL} (베타와 동일 — 사내망 한정)

■ 권한 변경
  - 베타 권한 {BETA_ROLE} → PROD 권한 {PROD_ROLE} (변경 시만 표기)

■ 데이터
  - 베타 기간 audit log + schedule 데이터 보존 (REQ-NF-SEC-004, 3년+)
  - 99999-SAMPLE 테스트 데이터는 T0 cutover 시 cleanup_99999_samples() 자동 삭제

문의: {SUPPORT_CONTACT}
IT_OPS 팀 드림
```

---

## 2. 신규 PROD 사용자 22명 — 사번 발급 일정

### 2.1 발급 schedule (placeholder)

| 단계 | 일자 (placeholder) | 작업 | 책임 |
|---|---|---|---|
| 사번 할당 | {LAUNCH_DATE} - 14d | Keycloak 사용자 22명 생성 (00000009 ~ 00000030) | IT_OPS |
| 초기 PIN 발급 | {LAUNCH_DATE} - 10d | V037 시드 패턴 동일 (4자리 무작위) — 봉인 봉투 개별 배포 | IT_OPS |
| 권한 매핑 | {LAUNCH_DATE} - 7d | Keycloak role 부여 (PLANNER / STK_USER / IT_OPS / READ_ONLY) | IT_OPS |
| 매뉴얼 배포 | {LAUNCH_DATE} - 7d | USER_MANUAL v1.5 PDF link + 메일 발송 | 개발 |
| 사전 안내 | {LAUNCH_DATE} - 3d | [PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE](PROD_LAUNCH_ANNOUNCEMENT_TEMPLATE_v1.0.md) §1 메일 발송 | IT_OPS |
| 첫 로그인 | {LAUNCH_DATE} | NFR-SEC-007 강제 PIN 변경 + 매뉴얼 로그인 시연 1회 (소그룹 5명 × 5회) | IT_OPS + 개발 |

### 2.2 권한 분배 (placeholder, S25-B 확정)

| Role | 베타 8명 | 신규 22명 | PROD 30명 합계 |
|---|---|---|---|
| PLANNER | 3 | TBD | TBD |
| STK_USER | 2 | TBD | TBD |
| IT_OPS | 2 | TBD | TBD |
| READ_ONLY | 1 | TBD | TBD |
| **합계** | **8** | **22** | **30** |

→ 사내 IT 협의 확정 후 v1.1 갱신.

---

## 3. 매뉴얼 — USER_MANUAL v1.5 link

- **위치**: {MANUAL_URL} (placeholder — Phase 3 산출물 등록 예정)
- **베타 → v1.5 갱신점**:
  - MES degraded mode 알림 화면 (BR-X06)
  - Drawer 알림 UI (Sprint 18)
  - PIN 변경 강제 화면 (NFR-SEC-007)
  - 30명 사용자 환경 권장 사용 패턴 (동시 접속 / 새로고침 주기)

---

## 4. 베타 운영 종료 일자

- **베타 운영 시작**: {BETA_START_DATE} (Sprint 19 EP-BETA-LAUNCH cutover)
- **베타 운영 종료**: {BETA_END_DATE} (placeholder — S25-B Go 결정 후 확정)
- **PROD 운영 시작**: {LAUNCH_DATE} = {BETA_END_DATE} + 1d (무중단 연속 운영, Blue/Green 사용)

---

## 5. Go/No-Go 결정 결과 (S25-B placeholder)

| 항목 | 결과 | 비고 |
|---|---|---|
| Go/No-Go 결정 일자 | {GO_NOGO_DATE} | placeholder |
| 결정 | TBD (Go / Conditional Go / No-Go) | S25-B 단계 |
| KPI 30일 결과 충족 | TBD | HTTP p95 < 2s / MES degraded < 5건 / audit 누락 0 |
| 결정자 (dual-review) | TBD / TBD | BR-X05 작성자 ≠ 승인자, ROLE_PLANNER 또는 IT_OPS lead |
| 조건부 Go 시 보류 항목 | TBD | (조건부 Go 시 작성, 만족 후 PROD 진입) |
| 발효 일자 | {LAUNCH_DATE} | Go 결정일 + 7d 이상 권장 |

---

## 6. 제약 및 placeholder 식별

본 v1.0 은 **data-free 템플릿** — 다음 항목은 사내 IT 협의 / 베타 결과 입력 후 v1.1 에서 확정:

1. {LAUNCH_DATE}, {BETA_END_DATE}, {GO_NOGO_DATE} — 일자 placeholder
2. Slack 채널명 / webhook URL — Phase 5+ cost-zero 재판단 후
3. Keycloak 실 endpoint (SAML/OIDC URL) — IT_OPS 사내 IdP 확정 시
4. 신규 22명 권한 분배 — 생산본부 협의
5. KPI 30일 결과 / k6 30명 결과 / Blue/Green dry-run 결과 — S25-B 입력
6. 베타 사용자 8명 운영 사용 빈도 — KPI 보고서 reference

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-06-01 | Claude Code | 초안 (data-free) — Sprint 25 S25-A ST-PROD-4. 베타 8명 사번 유지 + PIN 재설정 정책 (last_pin_change_at -31d) + 신규 22명 발급 일정 + 매뉴얼 v1.5 link + 베타 종료 일자 + Go/No-Go 결정 결과 placeholder. 실 발송 = S25-B Go 결정 후. |
