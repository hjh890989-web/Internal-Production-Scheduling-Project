# Epic Overview — [EP-19] 임의 시점 마스터 복원 UI (C-02)

**Sprint**: S5 | **Priority**: Could | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §8 EP-19 인용: "ST-19-1 timestamp 선택 복원 (5초 이내)"
> SRS REQ-FUNC-OC-014·XT-002: "사용자가 timestamp를 선택해 해당 시점 마스터 상태 5초 이내 복원."

본 Epic은 **EP-11 audit 활용 forensic** — 임의 과거 시점의 마스터 상태 재구성. audit row의 JSONB diff를 역재생. UI 시점 슬라이더 + 미리보기. 실제 복원은 별도 confirm (위험 방지).

**Why Sprint 5 Could**:
- 비상 시점 (마스터 손상·실수 입력) 복원 — 운영 안전성
- EP-11 audit 자산 활용 — Phase 1 investment ROI
- 5년 부하 — audit 데이터 양 시험

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-19-1](ST-19-1/_Story_Overview.md) | timestamp 선택 복원 (5초 이내) | 3 | ~2.1 | T-U + T-I + T-P | ☐ |

---

## Epic 레벨 DoD

- [ ] **`MasterTimeTravelService`** — audit 기반 점진적 reverse
- [ ] **API**: `GET /api/v1/master/snapshot?at={timestamp}` — 시점 상태
- [ ] **API**: `POST /api/v1/master/restore` Master Admin only (위험)
- [ ] **UI 시점 슬라이더** — 1주 / 1개월 / 1년 zoom
- [ ] **복원 미리보기** — 변경될 row 표시 (실제 복원 전)
- [ ] **5년 부하 측정**: 5년치 audit (10^6+ row)에서 5초 이내
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-19
- **PDD**: C-02 마스터 복원
- **SRS REQ-FUNC**: REQ-FUNC-OC-014, REQ-FUNC-XT-002
- **TestPlan**: TC-OC-014, TC-XT-002
- **선행**: EP-11 (audit)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
