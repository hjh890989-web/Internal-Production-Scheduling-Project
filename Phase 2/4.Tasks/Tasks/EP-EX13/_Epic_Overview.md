# Epic Overview — [EP-EX13] 성형 변경 자동 트리거 (v1.2 명시화 — REV-D-003)

**Sprint**: S3~S4 (carry-over) | **Priority**: Must ⭐ | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §6 EP-EX13 인용: "`vc.changed` 이벤트 수신 시 수동 호출 없이 영향 EX row를 재계산해야 한다."
> SRS REQ-FUNC-EX-013 / BR-X03 / BR-E11: "VC 스케줄 변경 시 영향받는 EX row를 자동 식별 + partial replan. 수동 호출 금지."

본 Epic은 Sprint 3 carry-over (EP-10 선행 필요 → S4 진입). 성형 confirmed 후 재변경 시 압출 후보 자동 갱신. 변경 종류 (수량·날짜·hose) 별로 영향 분석 → 최소 영향 EX row만 재계산 (전체 replan 회피).

**Why Sprint 3~4**:
- **REV-D-003 명시화** — 수동 호출 제거 (BR-X03 자동화)
- **EP-10 선행** — Confirmed 상태에서만 발생 가능 → S4 진입
- **EP-EX14 진입 전제** — 변경 PUSH 입력원

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-EX13-1](ST-EX13-1/_Story_Overview.md) | `vc.changed` 이벤트 자동 재계산 (수동 호출 금지) | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`VcChangedEvent`** publisher + subscriber (Spring Modulith)
- [ ] **영향 EX row 식별**: hose_id + production_date 범위 (deadline 기준)
- [ ] **Partial replan** — 변경된 hose만 재계산 (전체 replan 회피)
- [ ] **100건 시뮬**: 100% 자동 재계획 (수동 호출 0건)
- [ ] **audit** 자동 재계산 흔적 기록
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §6 EP-EX13
- **SRS REQ-FUNC**: REQ-FUNC-EX-013
- **BR**: BR-X03 (수동 호출 금지), BR-E11 (변경 영향 자동 분석)
- **TestPlan**: TC-EX-013
- **선행**: EP-10 (Confirmed 게이트), EP-EX11 (검증 게이트), EP-EX12 (충돌 대안)
- **후행**: EP-EX14 (PUSH)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-EX13 |
