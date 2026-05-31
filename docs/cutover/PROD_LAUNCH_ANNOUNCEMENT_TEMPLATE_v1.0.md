# PROD 정식 운영 공지 템플릿 v1.0

**대상**: 송우산업 사내 공정 스케줄링 시스템 베타 → 정식 운영 전환 공지 (S25-A ST-PROD-4 — data-free)
**작성일**: 2026-06-01 | **버전**: 1.0 | **참조**: Sprint 25 EP-PROD-LAUNCH / SRS v1.5 NFR-SEC-007 / USER_MANUAL v1.5
**발송 시점**: 베타 1개월 + Go/No-Go 결정 후 (S25-B 단계). 본 v1.0 은 **템플릿 (placeholder 다수)** — 실 발송 전 사내 IT 협의 결과로 변수 확정 후 사용.

---

## 공통 변수 (3 채널 공통)

| 변수 | 설명 | 예시 |
|---|---|---|
| `{LAUNCH_DATE}` | 정식 운영 개시일 (YYYY-MM-DD) | 2026-07-01 |
| `{RECIPIENT_NAME}` | 수신자 이름 | 홍길동 |
| `{EMPLOYEE_ID}` | 사번 8자리 (NFR-SEC-007) | 00000009 |
| `{INITIAL_PIN}` | 초기 PIN 4자리 (NFR-SEC-007, 첫 로그인 강제 변경) | 1234 |
| `{ROLE}` | Spring authority 매핑 — PLANNER / STK_USER / IT_OPS / READ_ONLY | PLANNER |
| `{ACCESS_URL}` | 사내망 접속 URL | http://schedule.intranet |
| `{MANUAL_URL}` | 사용자 매뉴얼 v1.5 link | http://schedule.intranet/docs/USER_MANUAL_v1.5.pdf |
| `{SUPPORT_CONTACT}` | IT_OPS 사번 + 사내 IM | IT_OPS 90000001 / 사내 IM |
| `{BETA_END_DATE}` | 베타 종료일 | 2026-06-30 |
| `{HELPDESK_HOURS}` | 헬프데스크 운영 시간 (KST, BR-X04) | 평일 09:00 ~ 18:00 |

---

## §1 사내 메일 (Email)

> **수신자**: 전 직원 (생산본부 + IT_OPS + 임원) — 베타 8명 + 신규 22명 = 총 30명 + 임원 cc

```
제목: [공지] 사내 공정 스케줄링 시스템 정식 운영 시작 ({LAUNCH_DATE})

{RECIPIENT_NAME} 님,

송우산업 사내 공정 스케줄링 시스템이 1개월 베타 운영을 마치고
{LAUNCH_DATE} 부터 정식 운영을 시작합니다.

■ 운영 개요
  - 대상: 저압가류기 4대 + IC가류기 1대 + 압출 (4-shift × 75%)
  - 47품번 × 1주 horizon · 약 1500 row 자동 스케줄링
  - 영림원 ERP 와 분리 (사내 한정 운영, 클라우드 미사용)

■ 접속 정보
  - URL: {ACCESS_URL}
  - 사번: {EMPLOYEE_ID} (8자리)
  - 초기 PIN: {INITIAL_PIN} (4자리)
  - 권한(role): {ROLE}

■ 보안 안내 (NFR-SEC-007)
  - 첫 로그인 시 PIN 4자리 변경 강제
  - 5회 연속 실패 시 10분 잠금
  - PIN 90일 주기 변경 권장 (시스템이 알림)
  - 본인 사번/PIN 타인 공유 금지 (audit BR-X02 기록)

■ 매뉴얼
  - 사용자 매뉴얼 v1.5: {MANUAL_URL}
  - 베타 운영 중 갱신된 신규 기능 (MES degraded mode 알림, Drawer 등)

■ 문의
  - IT_OPS: {SUPPORT_CONTACT}
  - 헬프데스크 운영시간: {HELPDESK_HOURS} (KST)
  - 장애 발생 시 사내 IM #ops-helpdesk 우선

■ 베타 → 정식 전환 안내
  - 베타 사용자 8명 (00000001 ~ 00000008): 사번 유지, PIN 재설정 강제
  - 자세한 사항은 별첨 BETA_TO_PROD_TRANSITION_v1.0.md 참조

감사합니다.
IT_OPS 팀 드림
```

---

## §2 사내 IM (사내 메신저, 200자 요약형)

> **채널**: 전사 공지 채널 + 생산본부 채널 동시 게시

```
[공지] 사내 공정 스케줄링 시스템 정식 운영 시작

{LAUNCH_DATE} 부터 정식 운영합니다.
- 접속: {ACCESS_URL}
- 로그인: 사번 8자리 + 초기 PIN 4자리 (개별 메일 발송됨)
- 첫 로그인 시 PIN 변경 강제 (NFR-SEC-007)
- 매뉴얼: {MANUAL_URL}
- 문의: {SUPPORT_CONTACT}

베타 운영 ({BETA_END_DATE} 종료) → 정식 전환.
베타 사용자도 PIN 재설정 필요.
```

(약 200자 내외, 사내 IM 가독성 최우선)

---

## §3 Slack #ops-announce (Phase 5+ placeholder)

> **상태**: Sprint 20 ST-EXT-1 cost-zero 정책으로 Slack webhook **미발급** 상태. Phase 5+ 재판단 시점 (사용자 30명 초과 / 외부 협력사 합류) 실 webhook 발급 후 활성.
> **현 운영**: 본 §3 비활성 → §2 사내 IM 로 **대체 발송**.

```json
{
  "channel": "#ops-announce",
  "username": "SchedulingBot",
  "icon_emoji": ":factory:",
  "text": ":mega: *사내 공정 스케줄링 시스템 정식 운영 시작*",
  "attachments": [
    {
      "color": "#36a64f",
      "fields": [
        { "title": "개시일", "value": "{LAUNCH_DATE}", "short": true },
        { "title": "URL", "value": "{ACCESS_URL}", "short": true },
        { "title": "로그인", "value": "사번 8자리 + PIN 4자리 (NFR-SEC-007)", "short": false },
        { "title": "매뉴얼", "value": "{MANUAL_URL}", "short": false },
        { "title": "문의", "value": "{SUPPORT_CONTACT}", "short": false }
      ],
      "footer": "Internal Production Scheduling | BR-X04 KST",
      "ts": "{UNIX_TS}"
    }
  ]
}
```

활성 조건 (모두 충족 시):
1. Sprint 20 ST-EXT-1 webhook URL 실 발급 (현 placeholder)
2. 사내 Slack workspace 정식 도입 (현 미도입)
3. `#ops-announce` 채널 생성 + IT_OPS 멤버 가입
4. 본 템플릿 Sprint 26+ 에서 v1.1 로 업그레이드

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-06-01 | Claude Code | 초안 (data-free) — Sprint 25 S25-A ST-PROD-4. 3 채널 (메일/사내 IM/Slack) 템플릿 + 공통 변수 10건. Slack 은 Phase 5+ placeholder 유지. 실 발송 = S25-B Go 결정 후. |
