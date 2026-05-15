# Story Overview — [EP-08] ST-08-3 압출 필요 수량 `Q_ext` 계산

**Sprint**: S3 | **Epic**: EP-08 압출 수식 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §6 EP-08 ST-08-3: "TK-08-3-1 `Q_ext = max(0, Q_vc + target - current)`, TK-08-3-2 4종 재고 케이스 단위 테스트, TK-08-3-3 통합 테스트"
> SRS REQ-FUNC-EX-010: "압출 필요 수량 Q_ext는 max(0, Q_vc + target_stock - current_stock). 재고가 target보다 많으면 0."

본 Story는 압출 후보 수량의 **재고 반영 산출**. 성형 필요량(Q_vc) + 안전재고(target) − 현재고(current). 음수면 0 (이미 충분). EP-EX11 (검증 게이트)에서 yield 합산 ≥ Q_ext 충족 여부 판정.

**Why Must**:
- Q_ext 0건 시 압출 후보 생성 skip — capa 낭비 방지
- 재고 4 케이스 (모두 정상·target만·current만·둘 다 음수) 모두 deterministic
- EP-EX11 진입 전 정확한 Q_ext 공급 필수

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-08-3-1](TK-08-3-1.md) | `Q_ext = max(0, Q_vc + target - current)` 구현 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-08-3-2](TK-08-3-2.md) | 4종 재고 케이스 단위 테스트 | 0.4 | Backend + QA | T-U | ☐ |
| [TK-08-3-3](TK-08-3-3.md) | 통합 테스트 (재고 마스터 + ExScheduleCandidate 연동) | 0.5 | Backend + QA | T-I + A | ☐ |

> **선행**: [ST-08-1](../ST-08-1/_Story_Overview.md), [ST-08-2](../ST-08-2/_Story_Overview.md)
> **후행**: EP-EX11 ST-EX11-1 (Q_ext 입력 사용)

---

## Story 레벨 DoD

- [ ] **`ExtrusionDemandCalculator.computeQExt(qVc, targetStock, currentStock)`** — `max(0, qVc + target − current)`
- [ ] **재고 마스터 조회** — `master.product_inventory(hose_id, target_stock, current_stock, updated_at)`
- [ ] **4 케이스 단위 테스트**: (1) 충분한 재고, (2) target만 도달, (3) current 부족, (4) 음수 입력 방어
- [ ] **통합 테스트** — VC 확정 → Q_ext 계산 → ExScheduleCandidate 수량 정합
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-08 ST-08-3
- **PDD-03**: M-08 §4 A3
- **SRS REQ-FUNC**: REQ-FUNC-EX-010
- **TestPlan**: TC-EX-010

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-08 ST-08-3 |
