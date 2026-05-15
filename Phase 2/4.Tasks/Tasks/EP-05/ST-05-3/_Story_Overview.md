# Story Overview — [EP-05] ST-05-3 필요 수량 계산 + 회전 배치 알고리즘

**Sprint**: S2 | **Epic**: EP-05 회전수 배치 (M-05) | **Priority**: Must ⭐⭐
**SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Story 목적

> WBS §5.2 EP-05 인용: "ST-05-3 — TK-05-3-1 Q_required 계산, TK-05-3-2 회전 배치 알고리즘 v1 (단순 greedy), TK-05-3-3 100건 회귀 위반 0건"
> SRS REQ-FUNC-VC-009 인용: "시스템은 품번별 `Q_required = max(0, Q_net + target_stock − current_stock)`을 계산해야 한다."
> SRS REQ-FUNC-VC-010 인용: "시스템은 품번별 (가류기, 슬롯, 회전) 튜플을 할당하여 누적 yield가 기한 이전 Q_required를 충족하도록 해야 한다."

본 Story는 **EP-05의 핵심·Sprint 2의 가시 산출물**. 모든 선행 컴포넌트(슬롯 매트릭스·yield·앵글 capa·회전 격자)를 결합한 **회전 배치 알고리즘 v1(단순 greedy)**. P1·P4가 1주 분량 수주 입력 → 후보 스케줄 생성을 직접 체감하는 단계. INT-2(이수진 반장)의 *"사무실에서 짠 스케줄을 종이에 다시 쓴다"* — 본 알고리즘이 현실적 후보를 생성하여 reverse work 없애는 목표.

**Why 본 Story가 Sprint 2 최우선인가**:
- **REQ-FUNC-VC-010 100건 회귀 0 위반** — Sprint 2 DoD 1순위
- **EXP-3 검증 직접 입력** — 그리디 baseline (앵글 교체 최소화 비교 기준)
- **NS-S03 P4 단독 사이클 80%** — 본 알고리즘 성숙도 직결

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-05-3-1](TK-05-3-1.md) | Q_required 계산 (재고·목표재고 결합) | 0.8 | Backend | T-U + A | ☐ |
| [TK-05-3-2](TK-05-3-2.md) | 회전 배치 알고리즘 v1 (단순 greedy) | 1.5 | Backend | T-U + T-L | ☐ |
| [TK-05-3-3](TK-05-3-3.md) | 100건 회귀 위반 0건 + 5분 SLA | 1.2 | QA + Backend | T-I + T-L | ☐ |

> **선행 의존**: ST-04-1·2·3 (슬롯 매트릭스·Unschedulable), ST-05-1·2 (회전 격자·yield·앵글 capa)
> **후행 차단**: ST-05-4 (저압/IC 라우팅 — 본 알고리즘 내부 호출), EP-06 (D-2 역산은 알고리즘 출력에 적용), EP-07 압출 (성형 확정 → D-1 자동 역산)

---

## Story 레벨 DoD

- [ ] **Q_required = max(0, Q_net + target_stock − current_stock)** 정확 (TC-VC-009)
- [ ] **회전 배치 알고리즘 v1** 동작 — 슬롯 O/X + 앵글 capa + 일일 capa 모두 충족
- [ ] **100건 회귀 위반 0건** (TC-VC-010)
- [ ] **5분 p95 SLA** (NFR-PER-002, 1주 호라이즌 47품번 기준)
- [ ] **앵글 교체 횟수 baseline** 측정 — ST-05-4·ST-21-x에서 최적화 대상
- [ ] 단위 + 부하 테스트 ≥ 80% 커버리지
- [ ] Sprint Review 데모: 1주 수주 → 후보 스케줄 (간트차트 + JSON Export)

---

## References

- **WBS Story**: §5.2 EP-05 ST-05-3
- **SAD**: §4 컴포넌트 — OptimizerService · CalcSvc
- **SRS REQ-FUNC**: REQ-FUNC-VC-009·010
- **SRS REQ-NF**: REQ-NF-PER-002 (≤5분), REQ-NF-KPI-004 (S-03 단독 80%)
- **PDD-02 v1.2**: §4 A4 회전 배치
- **TestPlan**: TC-VC-009 (4종 재고), TC-VC-010 (100 위반 0), TC-PER-002 (5분 SLA)
- **연관**: 선행 [ST-04-1](../../EP-04/ST-04-1/_Story_Overview.md), [ST-05-1](../ST-05-1/_Story_Overview.md), [ST-05-2](../ST-05-2/_Story_Overview.md), 후속 [ST-05-4](../ST-05-4/_Story_Overview.md), EP-06, EP-07

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-05 ST-05-3 + REQ-FUNC-VC-009·010 + NFR-PER-002 |
