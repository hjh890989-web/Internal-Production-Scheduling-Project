# Story Overview — [EP-VC16] ST-VC16-1 전체 스케줄 제약 검사 API ≤ 3초 p95

**Sprint**: S2~S3 | **Epic**: EP-VC16 On-Demand 전체 스케줄 검사 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.2 EP-VC16 ST-VC16-1: "TK-VC16-1-1 `/api/v1/schedule/validate-all` 엔드포인트, TK-VC16-1-2 1주 호라이즌 부하 시나리오, TK-VC16-1-3 p95 ≤3초 측정 + 회귀"
> SRS REQ-FUNC-VC-016: "On-demand 전체 스케줄 검사 ≤ 3초 p95."

본 Story는 기 확정 VcSchedule 전체 row를 일괄 검증. RulePipeline 재사용 — Allocator와 동일 엔진. 캐시 hit ratio 활용 + parallel stream으로 p95 ≤ 3초 달성.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-VC16-1-1](TK-VC16-1-1.md) | `/api/v1/schedule/validate-all` 엔드포인트 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-VC16-1-2](TK-VC16-1-2.md) | 1주 호라이즌 부하 시나리오 + 회귀 | 0.4 | QA + Backend | T-I + A | ☐ |
| [TK-VC16-1-3](TK-VC16-1-3.md) | p95 ≤ 3초 측정 + 성능 회귀 | 0.5 | QA + Backend | T-P + A | ☐ |

> **선행**: EP-05, EP-21
> **후행**: Frontend "검사 실행" 버튼 (Sprint 3+)

---

## Story 레벨 DoD

- [ ] `ScheduleValidatorService.validateAll(scheduleId)` — RulePipeline 재사용
- [ ] API: `POST /api/v1/schedule/validate-all/{scheduleId}` (mutating)
- [ ] **p95 ≤ 3초** (1주 호라이즌 ≈ 1,500 row 검증)
- [ ] **모든 위반 함께 반환** — early-exit 금지 (전체 점검)
- [ ] 결과 summary: 카테고리별 카운트 + total

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-VC16 ST-VC16-1
- **SRS REQ-FUNC**: REQ-FUNC-VC-016
- **TestPlan**: TC-VC-016

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
