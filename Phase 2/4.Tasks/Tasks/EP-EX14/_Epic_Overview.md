# Epic Overview — [EP-EX14] 압출 패드 WebSocket PUSH (v1.2 명시화 — REV-D-003)

**Sprint**: S4 | **Priority**: Must ⭐ | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §6 EP-EX14 인용: "ST-EX14-1 WebSocket PUSH p95 ≤2초 (압출 패드). TK-EX14-1-1 STOMP @ /ws 채널, TK-EX14-1-2 Redis Pub/Sub 백업 경로, TK-EX14-1-3 soak 테스트"
> SRS REQ-FUNC-EX-014: "성형 변경 PUSH를 WebSocket으로 압출 패드에 2초 p95 이내 전달."

본 Epic은 EP-EX13의 partial replan 결과를 **현장 압출 패드**에 즉시 PUSH. EP-VC15·EX12 충돌 발견 + EP-EX13 자동 replan 결과를 작업자가 30초 이내 알 수 있도록. STOMP over WebSocket + Redis Pub/Sub fallback (단일 인스턴스지만 향후 확장 대비).

**Why Sprint 4 마무리**:
- **REV-D-003 명시화** — UX 완결성 (변경 즉시 통보)
- **EP-EX13 결합** — vc.changed → replan → PUSH chain
- **EP-30 (성형 PUSH)** 와 페어 — 양 공정 일관 UX

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-EX14-1](ST-EX14-1/_Story_Overview.md) | WebSocket PUSH p95 ≤ 2초 (압출 패드) | 3 | ~2.1 | T-U + T-I + T-P | ☐ |

---

## Epic 레벨 DoD

- [ ] **STOMP `/ws/extrusion`** 엔드포인트 + Spring WebSocket
- [ ] **Redis Pub/Sub** fallback (Phase 2+ 다중 인스턴스 대비)
- [ ] **EX 패드 구독**: `topic/extrusion-updates`
- [ ] **p95 ≤ 2,000ms** — soak 테스트 30분
- [ ] **중앙값 ≤ 1,500ms**
- [ ] EP-EX13 chain — vc.changed → replan → PUSH
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: §6 EP-EX14
- **SRS REQ-FUNC**: REQ-FUNC-EX-014
- **PDD-04**: PUSH 통신
- **TestPlan**: TC-EX-014
- **선행**: EP-EX13, EP-30 (성형 PUSH 패턴)
- **후행**: Sprint 5 UI 압출 패드

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
