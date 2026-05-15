# Epic Overview — [EP-16] 카톡 백업 채널 (S-04)

**Sprint**: S5 | **Priority**: Should | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §8 EP-16 인용: "ST-16-1 카카오톡 BizMessage 보강 + 도달 로그"
> SRS REQ-FUNC-OC-010·CO-008: "이메일 미수신 시 카카오톡 BizMessage 폴백. 도달 상태 100% 기록."

본 Epic은 EP-03 (Diff 알림) 의 보조 채널. 이메일 실패·미열람 시 카카오톡으로 폴백. 도달 상태 (delivered·read·failed) 기록 → 알림 SLA 추적성.

**Why Sprint 5**:
- **EP-03 알림 SLA 완성** — 이메일 + 카카오톡 dual-channel
- **현장 ↔ 사무실 즉시 통신** — 모바일 친화
- **NS-04 알림 도달률** KPI

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-16-1](ST-16-1/_Story_Overview.md) | 카카오톡 BizMessage 보강 + 도달 로그 | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`KakaoBizMessageService`** — Kakao Biz API 통합 (Phase 1 검증된 vendor)
- [ ] **`notification_delivery_log`** — channel·status·timestamp 기록
- [ ] **Fallback 정책**: 이메일 1분 미열람 → 카카오톡 발송
- [ ] **도달 상태 100% 채움**: SENT·DELIVERED·READ·FAILED
- [ ] **NS-04 KPI**: 알림 도달률 ≥ 95%
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-16
- **PDD**: S-04 백업 채널
- **SRS REQ-FUNC**: REQ-FUNC-OC-010, REQ-FUNC-CO-008
- **TestPlan**: TC-OC-010
- **선행**: EP-03 (이메일 알림)
- **후행**: 없음

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
