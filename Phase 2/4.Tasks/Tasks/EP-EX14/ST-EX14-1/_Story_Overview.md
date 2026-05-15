# Story Overview — [EP-EX14] ST-EX14-1 WebSocket PUSH p95 ≤ 2초 (압출 패드)

**Sprint**: S4 | **Epic**: EP-EX14 압출 패드 WebSocket PUSH | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §6 EP-EX14 ST-EX14-1: "TK-EX14-1-1 STOMP @ /ws 채널, TK-EX14-1-2 Redis Pub/Sub 백업 경로, TK-EX14-1-3 soak 테스트"

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-EX14-1-1](TK-EX14-1-1.md) | STOMP @ /ws/extrusion 채널 + Spring WebSocket | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-EX14-1-2](TK-EX14-1-2.md) | Redis Pub/Sub 백업 경로 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-EX14-1-3](TK-EX14-1-3.md) | Soak 테스트 (중앙값 ≤ 2s, p95 ≤ 2s) | 0.7 | QA + Backend | T-P + A | ☐ |

> **선행**: [EP-EX13](../../EP-EX13/), [ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md) (Redis)
> **후행**: Sprint 5 UI

---

## Story 레벨 DoD

- [ ] **`@MessageMapping("/extrusion-updates")`** Spring WebSocket controller
- [ ] **STOMP endpoint** `/ws/extrusion` (Keycloak auth)
- [ ] **Redis Pub/Sub** topic `extrusion-updates` (fallback)
- [ ] **EP-EX13 chain**: ReplanService → PushService → STOMP broker
- [ ] **Soak 30분**: 중앙값 ≤ 1.5s, p95 ≤ 2s
- [ ] **연결 끊김 → 자동 재연결** (frontend 책임 — backend는 idempotent)
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: §6 EP-EX14 ST-EX14-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-014
- **TestPlan**: TC-EX-014

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
