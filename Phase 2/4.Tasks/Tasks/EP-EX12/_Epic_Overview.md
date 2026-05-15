# Epic Overview — [EP-EX12] 압출 충돌 대안 (v1.2 명시화 — REV-D-003)

**Sprint**: S3 (압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Epic 목적

> WBS §6 EP-EX12 인용: "ST-EX12-1 G_VAL 실패 시 ≥3 대안 (조기 시작·야간 후반·납기 협상·외주). TK-EX12-1-1 대안 생성 알고리즘, TK-EX12-1-2 모든 실패 케이스 ≥3 대안 회귀"
> SRS REQ-FUNC-EX-012: "게이트 실패 시 더 일찍 시작, 야간 후반 활용, 성형 투입일 협상, 외주 등의 대안을 ≥3개 제시"

본 Epic은 EP-EX11 검증 게이트 실패 시 **압출 도메인 특화 대안** 제시. EP-VC15 (성형 충돌 대안)와 같은 패턴 — `Categorizer + Generator` 구조 — 그러나 압출 대안은 도메인별 차별화:
- **조기 시작** (EARLIER_START) — 전일 야간 후반 활용
- **야간 후반** (NIGHT_SECOND_BOOST) — NIGHT_SECOND shift 추가 배치
- **성형 투입일 협상** (VC_DATE_NEGOTIATE) — vc_date 1일 연장 → ex_deadline +1
- **외주 처리** (OUTSOURCE)

**Why Must (Sprint 3)**:
- **REV-D-003 명시화** — EP-VC15 (성형) ↔ EP-EX12 (압출) 페어 완결
- **사용자 가시화** — 검증 실패 시 사용자가 즉시 의사결정 가능 (단순 fail 통보 회피)
- **EP-EX11 후속 자동화** — Gate fail → 대안 enrich → UI 노출

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-EX12-1](ST-EX12-1/_Story_Overview.md) | G_VAL 실패 시 ≥3 대안 (조기·야간 후반·납기·외주) | 2 | ~1.4 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`ExtrusionAlternativeGenerator`** — 4 base 대안 (EARLIER_START·NIGHT_SECOND_BOOST·VC_DATE_NEGOTIATE·OUTSOURCE)
- [ ] **카테고리별 정책 매트릭스** — 위반 종류(yield·capacity) × 대안 적용 가능 여부
- [ ] **회귀 100건** — 모든 fail 케이스 ≥ 3 distinct 대안
- [ ] **API**: `/api/v1/schedule/extrusion/conflicts/{candidateId}` p95 ≤ 1초
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-EX12
- **SRS REQ-FUNC**: REQ-FUNC-EX-012
- **PDD-03**: M-10 충돌 대안
- **TestPlan**: TC-EX-012
- **선행**: EP-EX11 (검증 게이트), EP-VC15 (대안 generator 패턴)
- **후행**: EP-EX13 (성형 변경 자동 트리거 — Sprint 3~4)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-EX12 + REQ-FUNC-EX-012 |
