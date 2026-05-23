# STK_USER — 현장 작업자 가이드

**Role**: STK_USER (Keycloak) | **권한 등급**: P3
**책임**: 시뮬뷰 조회 + swap 제안 (Planner 수용 대상) + 압출 패드 STOMP 알림 수신

> STK_USER 는 **현장 의견 제공자**. Planner 결정 전에 현장 우선순위·실효성 피드백.
> 모든 mutation 은 Planner 의 1클릭 수용을 거침 (BR-X05 dual-review 정합).

---

## 1. 접근 가능 화면

| 경로 | 화면 | 용도 |
|---|---|---|
| `/home` | Home | 진입점 |
| `/vc/simview` | 성형 시뮬뷰 (read + 제안) | 회전 격자 + swap 제안 |
| `/extrusion-matrix` | 압출 매트릭스 (read) | 실시간 STOMP push 수신 |
| `/audit/restore` | (제한) timeline read only |
| ❌ `/orders/import` | 권한 없음 |
| ❌ `/vc/capacity-queue` | 권한 없음 (BR-V12·V13 — PLANNER 단독, [BS-06](../beta-scenarios/06-capacity-overflow-kd-supplement.md) 참조) |

---

## 2. 핵심 액션 — swap 제안 (REQ-FUNC-VC-018)

### 2.1 제안 절차

```
1. /vc/simview 진입 (SSO)
2. 회전 격자 검토 — 현장 현실 vs 시스템 일정 비교
3. 변경 제안할 cell 우클릭 → "다른 회전으로 변경 제안" (Sprint 7+ UI)
   - 대안: 모바일 카카오 톡 — Planner DM
4. target rotation 선택 (같은 machine·slot·date 안 — 총량 보존 invariant)
5. 사유 입력 (필수):
   - 예: "전기 사용량 평준화"
   - 예: "동시 셋업 횟수 감소"
   - 예: "현장 작업자 인계 시간 확보"
6. POST /api/v1/schedule/vc/proposals (status=PROPOSED)
7. Planner 알림 자동 (in-app + Slack)
```

### 2.2 제안 후 대기

- PROPOSED 상태 — Planner 의 1클릭 수용 또는 거절 대기
- 알림 수신 (in-app 또는 카카오) — ACCEPTED / REJECTED 통보

---

## 3. 압출 패드 — STOMP 실시간 알림

`/extrusion-matrix` 진입 시 자동 STOMP `/topic/extrusion-updates` 구독:
- VC 변경 cascade 발생 시 즉시 grid 자동 갱신 (Planner 액션 0)
- 우상단 "마지막 cascade: HH:mm:ss (N건 갱신)" 표시
- 모바일 압출 패드 (Sprint 7+ Flutter) 도 동일 STOMP

---

## 4. swap 제안 통과율 KPI

- **목표** — 50% 이상 수용 (현장 의견 반영 척도)
- **낮은 통과율** 시 — 사유 구체성 개선 (Planner 가 reject 시 사유 확인)

---

## 5. FAQ

### Q1. 같은 slot 의 다른 angle 로 swap 제안 가능한가요?
**A**. 불가능. atomic swap 은 같은 (machine, slot, date) 안 rotation 만 교체. 다른 angle 변경은 Planner override.

### Q2. swap 제안이 거절되면 다시 제안할 수 있나요?
**A**. 가능. PROPOSED 상태 머신 — REJECTED 후 새 제안 (다른 target 또는 사유 보강).

### Q3. STOMP "disconnected" 표시 시?
**A**. 재로그인. Bearer JWT 만료 가능성. F12 → Network WS 확인.

### Q4. STK_USER 도 직접 일정 확정할 수 있나요?
**A**. 불가능. confirm 은 PLANNER role 단독. RBAC `@PreAuthorize hasRole('PLANNER')` 강제.

---

## 6. 비상 연락

| 상황 | 연락처 |
|---|---|
| 시스템 접근 불가 | 사내 IT 헬프데스크 |
| 일정 긴급 변경 통보 | Planner 카카오 DM |
| 압출 패드 알림 미수신 | IT_OPS — Slack `#scheduling-ops` |

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — STK_USER 페르소나 가이드 |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over — `/vc/capacity-queue` 권한 없음 cross-reference (BS-06 PLANNER 단독) |
