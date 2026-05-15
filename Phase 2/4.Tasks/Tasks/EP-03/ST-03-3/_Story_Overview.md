# Story Overview — [EP-03] ST-03-3 알림 발송 (시스템 + 카톡 백업)

**Sprint**: S1 | **Epic**: EP-03 Diff·알림 (M-03) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.1 EP-03 인용: "ST-03-3 — TK-03-3-1 카카오톡 BizMessage 클라이언트, TK-03-3-2 도달 상태 추적, TK-03-3-3 SLA <1분 부하 테스트"
> SRS REQ-FUNC-OC-009 인용: "Critical 변경 알림은 ≤1분, 일반 변경은 ≤5분 이내 영향받는 다운스트림(L3·L5)에 도달해야 한다. 100건 시뮬레이션 시 SLA ≥99% 도달."

본 Story는 EP-03의 최종 출력. ST-03-1·2의 결과를 **인앱 알림(WebSocket) + 카카오톡 BizMessage 백업** 2채널로 전송. **Critical은 ≤1분 SLA, Normal은 ≤5분**. INT-3 시각: P3(박도영 압출반장)이 *"카톡 누락으로 다음 날에야 알았다"* — 본 Story가 직접 해소.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-03-3-1](TK-03-3-1.md) | 카카오톡 BizMessage 클라이언트 + Webhook | 0.8 | Backend | T-I + I | ☐ |
| [TK-03-3-2](TK-03-3-2.md) | 도달 상태 추적 (sent·delivered·acknowledged·failed) | 0.6 | Backend | T-I + I | ☐ |
| [TK-03-3-3](TK-03-3-3.md) | SLA <1분 100건 부하 테스트 (k6) | 0.7 | QA + DevOps | T-L + A | ☐ |

> **선행 의존**: [ST-03-2](../ST-03-2/_Story_Overview.md) (severity 태깅), ST-00-2 (WebSocket 기반), EP-32 ST-32-x (Slack 통합)
> **후행 차단**: 없음 (Sprint 1 마지막 Story)

---

## Story 레벨 DoD

- [ ] **인앱 알림 (WebSocket STOMP)** — 모든 변경 Planner UI에 즉시 PUSH
- [ ] **카카오톡 백업** — Critical만 발송 (Normal은 인앱만)
- [ ] **Critical SLA ≤1분** 100건 시뮬레이션 ≥ 99% 도달
- [ ] **Normal SLA ≤5분** 동일 시뮬레이션 ≥ 99% 도달
- [ ] **도달 상태 추적** — `NOTIFICATION` 테이블에 sent/delivered/acknowledged/failed 기록
- [ ] **재시도 정책** — 실패 시 최대 3회 exponential backoff
- [ ] k6 부하 테스트 PASS — 30 동시 사용자, 100 critical 이벤트
- [ ] Sprint Review 데모: Critical 변경 → 인앱 + 카톡 1분 내 도달 시연

---

## References

- **WBS**: §5.1 EP-03 ST-03-3
- **SAD**:
  - §5.4 이벤트·메시징 (Spring ApplicationEvent + PG LISTEN/NOTIFY + Redis Pub/Sub)
  - §3.1 EXT-SYS-05 카카오톡 Workplace Bot
- **SRS REQ-FUNC**: REQ-FUNC-OC-009, REQ-FUNC-OC-010
- **SRS REQ-NF**: REQ-NF-PER-004 (PUSH ≤2초 = WebSocket 인앱), REQ-NF-KPI-015 (K-O04 SLA ≥99%)
- **PDD-01**: §4 A5 알림 발송
- **TestPlan**: TC-OC-009 (Critical SLA), TC-OC-010 (카카오톡 백업)
- **연관**: 선행 [ST-03-2](../ST-03-2/_Story_Overview.md), [ST-00-2](../../EP-00/ST-00-2/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.1 EP-03 ST-03-3 + REQ-FUNC-OC-009·010 + SLA <1분 |
