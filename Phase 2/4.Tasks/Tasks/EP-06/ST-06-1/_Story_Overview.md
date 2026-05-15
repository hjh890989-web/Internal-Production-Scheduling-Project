# Story Overview — [EP-06] ST-06-1 D-2 영업일 역산

**Sprint**: S2 (성형 핵심) | **Epic**: EP-06 납기 D-2 역산 (M-06) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-06 인용: "ST-06-1 — TK-06-1-1 영업일 캘린더 서비스, TK-06-1-2 D-2 역산 로직, TK-06-1-3 모든 row 완료일 ≤ 납기-2 검증"
> SRS REQ-FUNC-VC-008 / BR-X07 인용: "시스템은 성형 완료 기한을 `delivery_date − 2 영업일`로 설정해야 한다. 모든 schedule row 완료일 ≤ 납기 − 2 만족."

본 Story는 ST-05-3의 회전 배치 결과에 **납기 D-2 hard 제약**을 강제. INT-1 사건(P1: *"300개 납기 지연으로 식은땀"*)의 직접 차단. TK-05-1-2에서 stub로 둔 `WorkingCalendarService`를 정식 구현 — 영업일 계산 + 사내 휴일·법정공휴일 포함.

**Why 본 Story가 Sprint 2 핵심인가**:
- **BR-X07 hard 제약** — Sprint 2 DoD 강제 (다른 제약 충돌 시 비-납기 행 조정으로 해소)
- **EP-07 압출 D-1 역산** 의 인접 기반 — 본 캘린더 서비스를 EP-07도 재사용
- **NS-S05·NS-S06** D-Day·D-2 준수율 KPI 직결

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-06-1-1](TK-06-1-1.md) | 영업일 캘린더 서비스 (월~금 + 사내 휴일) | 0.8 | Backend | T-U + I | ☐ |
| [TK-06-1-2](TK-06-1-2.md) | D-2 역산 로직 + 회전 배치 통합 | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-06-1-3](TK-06-1-3.md) | 모든 row `완료일 ≤ 납기-2` 검증 + 회귀 | 0.6 | QA + Backend | T-I + A | ☐ |

> **선행 의존**: [ST-05-3](../../EP-05/ST-05-3/_Story_Overview.md) (GreedyRotationAllocator)
> **후행 차단**: EP-07 ST-07-1 (압출 D-1 역산은 본 캘린더 재사용), EP-VC15 (충돌 리포트가 D-2 위반도 카테고리화)

---

## Story 레벨 DoD

- [ ] **영업일 캘린더 서비스** — 월~금 기본 + 사내 휴일·법정공휴일 마스터 테이블 (`master.holiday`)
- [ ] **D-2 역산**: `vc_completion_deadline = delivery_date - 2 working days` (영업일 기준)
- [ ] **회전 배치 통합**: GreedyRotationAllocator가 본 deadline 강제
- [ ] **회귀 100건**: 모든 VcSchedule row의 production_date ≤ D-2 만족 (TC-VC-008)
- [ ] **BR-X07 hard 제약**: 다른 제약 충돌 시 비-납기 행 조정으로 해소 시도
- [ ] 단위 + 통합 테스트 ≥ 80% 커버리지

---

## References

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-06 ST-06-1
- **SAD**: §4 컴포넌트 — CalcSvc 백워드 플랜
- **SRS REQ-FUNC**: REQ-FUNC-VC-008 (D-2 deadline)
- **SRS REQ-NF**: REQ-NF-KPI-008 (K-V04 D-2 준수율 ≥98%)
- **BR**: BR-X07 (납기 hard 제약), BR-V09 (납기 D-2 우선)
- **PDD-02 v1.2**: §4 A2 T2.1 (역산)
- **TestPlan**: TC-VC-008 (D-2 회귀)
- **연관**: 선행 [ST-05-3](../../EP-05/ST-05-3/_Story_Overview.md), 후속 EP-07 ST-07-1 (압출 D-1)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-06 ST-06-1 + REQ-FUNC-VC-008 + BR-X07 + TC-VC-008 |
