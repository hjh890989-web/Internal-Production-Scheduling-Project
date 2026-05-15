# Epic Overview — [EP-08] 압출 수식 (M-08)

**Sprint**: S3 (압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 8 | **PD 추정**: ~5.6 PD

---

## Epic 목적

> WBS §6 EP-08 인용: "ST-08-1 4-shift + 75% 효율, ST-08-2 yield 수식 + BR-E05, ST-08-3 압출 필요 수량 Q_ext"
> SRS REQ-FUNC-EX-003~005·010 / BR-E03·E04·E05 / 리뷰 골든 케이스 인용:
> - "압출 효율 75%, 4-shift (주간전반·후반·야간전반·후반), 1 shift = 240 min × 0.75 = 180 min"
> - "yield = floor(speed × min × 1000 / length)"
> - "`29673-2R060` 주간전반 yield = 2,531 (BR-E05 reference case)"
> - "`Q_ext = max(0, Q_vc + target_stock - current_stock)` (재고 반영)"

본 Epic은 압출 후보 스케줄의 **수치 정확성** 핵심. EP-07이 deadline (시점), EP-08이 yield (수량) 산출. **`29673-2R060` BR-E05 회귀 = 2,531** 은 Sprint 3 DoD 단일 가장 중요한 검증 — 수식 오류는 압출 전체 신뢰도 붕괴.

**Why Sprint 3 핵심**:
- **BR-E05 reference case 통과** — 시스템 신뢰성 결정 지표 (사용자 인터뷰 INT-1 직결)
- **EP-09 (셋팅 그룹핑) 진입 전제** — yield 없이 그룹별 capacity 계산 불가
- **EP-EX11 (검증 게이트)** — yield 합산이 Q_ext 충족 여부 판정 입력

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-08-1](ST-08-1/_Story_Overview.md) | 4-shift 모델 + 75% 효율 | 3 | ~2.1 | T-U + T-I | ☐ |
| [ST-08-2](ST-08-2/_Story_Overview.md) | yield 수식 + BR-E05 reference 검증 | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-08-3](ST-08-3/_Story_Overview.md) | 압출 필요 수량 `Q_ext` 계산 | 2 | ~1.4 | T-U + T-I | ☐ |

---

## Epic 레벨 DoD

- [ ] **Shift 마스터** — 4 shift (주간전반·후반·야간전반·후반) × 75% 효율 = 180 min/shift
- [ ] **`/api/v1/master/shifts`** GET/POST/PUT (IT_OPS RBAC)
- [ ] **yield 수식**: `floor(speed_m_per_min × effective_min × 1000 / length_mm)`
- [ ] **`29673-2R060` 주간전반 yield = 2,531** 회귀 PASS (BR-E05)
- [ ] **단위 가드**: speed=m/min, length=mm — 혼동 시 명시적 에러
- [ ] **`Q_ext = max(0, Q_vc + target - current)`** — 재고 4종 케이스 검증
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-08
- **PDD-03**: M-08 수식
- **SRS REQ-FUNC**: REQ-FUNC-EX-003 (shift), EX-004 (효율), EX-005 (yield), EX-010 (Q_ext)
- **BR**: BR-E03 (4-shift), BR-E04 (75% 효율), BR-E05 (yield 수식)
- **TestPlan**: TC-EX-003·004·005·010
- **선행**: EP-07 (deadline 공급)
- **후행**: EP-09 (그룹핑), EP-EX11 (검증 게이트)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-08 + REQ-FUNC-EX-003~005·010 + BR-E03·E04·E05 |
