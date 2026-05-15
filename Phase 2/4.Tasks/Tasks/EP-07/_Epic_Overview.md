# Epic Overview — [EP-07] 압출 D-1 자동 역산 (M-07)

**Sprint**: S3 (압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §6 EP-07 인용: "성형 확정 → 압출 후보 자동 생성. ST-07-1 압출 완료 기한 = 성형 투입 - 1일, ST-07-2 영업일 캘린더 (월~금)."
> SRS REQ-FUNC-EX-001 / BR-E01 인용: "압출 완료 기한 = 성형 투입일 − 1 working day"
> SRS REQ-FUNC-EX-002 / BR-E02 / CON-10: "압출 영업일은 월~금 (주말 제외)"

본 Epic은 **Sprint 2 확정 VC 스케줄 → 압출 스케줄 자동 역산** 진입점. EP-06의 `WorkingCalendarService`를 그대로 재사용 (CON-10 자료 동일 캘린더). `vc.confirmed` 도메인 이벤트를 구독해 압출 완료 기한 자동 계산. 후속 EP-08 (수식)·EP-09 (그룹핑) 이 사용할 ExSchedule 후보의 `due_date` 기반 제공.

**Why Sprint 3 핵심**:
- **D-1 hard 제약** — 성형 D-Day와 1 영업일 간격 유지 (재공 정체 방지)
- **EP-06 캘린더 재사용** — 마스터 단일 (CON-10) 검증 토대 활용
- **EP-08 진입 전제** — 압출 후보 생성을 위한 due_date 공급
- **NS-S07** D-1 준수율 KPI 직결

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-07-1](ST-07-1/_Story_Overview.md) | 압출 완료 기한 = 성형 투입 - 1일 | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-07-2](ST-07-2/_Story_Overview.md) | 영업일 캘린더 (월~금) — 압출 적용 + 주말 회귀 | 2 | ~1.4 | T-U + T-I | ☐ |

---

## Epic 레벨 DoD

- [ ] **`vc.confirmed` 이벤트 구독** — 성형 확정 → 압출 D-1 역산 트리거
- [ ] **`BackwardExtrusionCalculator`** — `ex_completion_deadline = vc_production_date − 1 working day`
- [ ] **모든 ExSchedule row**: production_date ≤ deadline (BR-E01)
- [ ] **압출 영업일 = 월~금**: EP-06 캘린더 재사용 (`isWorkingDay`)
- [ ] **주말 vc_date 회귀**: `vc_date = 월요일` → `ex_deadline = 직전 금요일`
- [ ] **회귀 100건**: TC-EX-001·002 통과
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-07
- **PDD-03**: M-07 D-1 역산
- **SRS REQ-FUNC**: REQ-FUNC-EX-001·002
- **SRS REQ-NF**: REQ-NF-KPI-009 (NS-S07 D-1 준수율 ≥ 98%)
- **BR**: BR-E01 (D-1 hard 제약), BR-E02 (월~금), CON-10 (캘린더 단일)
- **TestPlan**: TC-EX-001·002
- **선행**: EP-06 (캘린더 서비스), EP-05 (VC 확정 이벤트 publisher)
- **후행**: EP-08 (압출 수식), EP-09 (셋팅 그룹핑), EP-EX11 (검증 게이트)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-07 + REQ-FUNC-EX-001·002 + BR-E01·E02 + CON-10 |
