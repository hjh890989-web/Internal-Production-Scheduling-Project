# Epic Overview — [EP-09] 압출셋팅 그룹핑 (M-09)

**Sprint**: S3 (압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §6 EP-09 인용: "ST-09-1 shift 내 무 셋업 + 셋팅 그룹 동시생산. TK-09-1-1 셋팅 번호(1~8) 그룹 모델, TK-09-1-2 shift당 단일 셋팅 그룹 강제, TK-09-1-3 4주 회귀 (shift 내 셋업 0건)"
> SRS REQ-FUNC-EX-006·007 / BR-E06·E07: "셋팅 번호 1~8별로 호환 가능 품번이 그룹화되며, shift 내에서 셋업 변경 금지 (BR-E06). 같은 셋팅 그룹 품번은 동일 shift에서 동시 생산 가능 (BR-E07)."

본 Epic은 EP-08 수식으로 산출된 압출 후보들을 **shift 단위로 그룹핑**해 셋업 변경 제거. 셋업 변경 1회 ≈ 30분 손실 (현장 실측), 일 24 회 shift 환경에서 셋업 0건 시 effective_min 100% 보전. 4주 회귀로 long-term 안정성 검증.

**Why Sprint 3 핵심**:
- **shift 내 셋업 0건** — 압출 생산성 직접 결정 (현장 실측 30% 효율 손실 회피)
- **셋팅 마스터 활용** — Phase 1 분석에서 47품번 → 1~8 셋팅 매핑 확정
- **EP-EX11 (검증 게이트)** 진입 전 그룹핑 정확성 확보

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-09-1](ST-09-1/_Story_Overview.md) | shift 내 무 셋업 + 셋팅 그룹 동시생산 | 5 | ~3.5 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`master.setting_group`** — 셋팅 번호 1~8 + 호환 품번 리스트
- [ ] **47품번 → setting_group 매핑 seed** (`Phase 1` 분석 데이터)
- [ ] **`SettingGroupAllocator`** — shift당 단일 셋팅 그룹 강제 (BR-E06)
- [ ] **동시 생산**: 같은 그룹 품번이 동일 shift에 배치 (BR-E07)
- [ ] **4주 호라이즌 회귀**: 모든 shift에서 셋업 0건 (TC-EX-006·007)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-09
- **PDD-03**: M-09 그룹핑
- **SRS REQ-FUNC**: REQ-FUNC-EX-006·007
- **BR**: BR-E06 (shift 내 무 셋업), BR-E07 (그룹 동시 생산)
- **TestPlan**: TC-EX-006·007
- **REF**: `Phase 1/2.Raw Materials/Extrusion/압출공정_제약조건.xlsx` (셋팅 번호 컬럼)
- **선행**: EP-08 (yield + Q_ext)
- **후행**: EP-EX11 (검증 게이트), EP-EX12 (충돌 대안)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-09 + REQ-FUNC-EX-006·007 + BR-E06·E07 |
