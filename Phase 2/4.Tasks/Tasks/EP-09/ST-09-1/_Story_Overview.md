# Story Overview — [EP-09] ST-09-1 shift 내 무 셋업 + 셋팅 그룹 동시생산

**Sprint**: S3 | **Epic**: EP-09 압출셋팅 그룹핑 | **Priority**: Must ⭐
**SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Story 목적

> WBS §6 EP-09 ST-09-1: "TK-09-1-1 셋팅 번호(1~8) 그룹 모델, TK-09-1-2 shift당 단일 셋팅 그룹 강제, TK-09-1-3 4주 회귀 (shift 내 셋업 0건)"
> SRS REQ-FUNC-EX-006·007 / BR-E06·E07: "shift 내 셋팅 변경 금지 + 같은 그룹 품번은 동일 shift에서 동시 생산 가능"

본 Story는 EP-08 산출 yield 후보들을 (shift, machine) 단위로 셋팅 그룹별 묶음. 한 shift에서 두 가지 셋팅 그룹 혼합 금지 — 셋업 변경 발생. 4주 호라이즌 회귀로 long-term 안정성 검증 (1주 회귀는 우연일 수 있음).

**Why Must (Sprint 3 핵심)**:
- 셋업 1회 30분 손실 — shift effective_min 180 대비 17% 효율 저하
- 47품번 × 8 셋팅 그룹 분포 — 운영팀 확인 (Phase 1)
- EP-EX11 검증 게이트 입력 — 그룹핑 후 yield 합산 ≥ Q_ext 판정

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-09-1-1](TK-09-1-1.md) | 셋팅 번호(1~8) 그룹 마스터 + 47품번 매핑 | 1.2 | Backend | T-U + I | ☐ |
| [TK-09-1-2](TK-09-1-2.md) | `SettingGroupAllocator` — shift당 단일 셋팅 그룹 강제 | 1.5 | Backend | T-U + T-I | ☐ |
| [TK-09-1-3](TK-09-1-3.md) | 4주 호라이즌 회귀 (shift 내 셋업 0건) | 0.8 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-08 ST-08-1·2·3](../../EP-08/)
> **후행**: EP-EX11, EP-EX12

---

## Story 레벨 DoD

- [ ] **`master.setting_group`** — `group_number` (1~8) + `group_name` + 호환 품번 리스트
- [ ] **`master.product_setting_group`** — `(hose_id, group_number)` PRIMARY KEY (M:N 매핑)
- [ ] **47품번 setting_group 매핑 seed** — Phase 1 분석 결과
- [ ] **`SettingGroupAllocator`** — shift 단위 동일 그룹 강제
- [ ] **4주 회귀**: 28일 × 24 shift × 4 EX 머신 = 2,688 shift slot에서 셋업 0건
- [ ] **다중 그룹 시나리오**: 같은 품번이 여러 그룹 호환 시 적절한 선택
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-09 ST-09-1
- **PDD-03**: M-09 §4 A1
- **SRS REQ-FUNC**: REQ-FUNC-EX-006·007
- **BR**: BR-E06·E07
- **TestPlan**: TC-EX-006·007 (4주 회귀)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-09 ST-09-1 |
