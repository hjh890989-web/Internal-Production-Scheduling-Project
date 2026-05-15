# Epic Overview — [EP-E2E] E2E 시뮬레이션 + 베타 준비

**Sprint**: S5 | **Priority**: Must ⭐⭐ (Sprint 5 DoD) | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §8 EP-E2E 인용: "E2E 1주 분량 시뮬레이션, 베타 그룹 시작 (4명)"
> EXP-1·2·5: "수주 → 성형 → 압출 cascade E2E 검증. 모든 납기 D-Day 충족. 베타 4명 1주 병행."

본 Epic은 **Phase 2 종합 검증** — 모든 Sprint 1~5 Epic의 chain 동작 확인. 수주 import (EP-01) → VC 후보 (EP-04·05·21) → 확정 (EP-10) → EX cascade (EP-EX13) → 시뮬뷰 (EP-15) → Excel export (EP-12) → 베타 사용 (실 사용자). Phase 3 진입 게이트.

**Why Sprint 5 핵심**:
- **EXP-1·5** — Phase 2 가장 큰 검증
- **베타 시작** — 실 사용자 피드백 → Phase 3 입력
- **모든 Sprint DoD 통합 검증**

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-E2E-1](ST-E2E-1/_Story_Overview.md) | E2E 1주 분량 시뮬레이션 | 3 | ~2.1 | T-I + A + E2E | ☐ |
| [ST-E2E-2](ST-E2E-2/_Story_Overview.md) | 베타 그룹 시작 (4명) | 2 | ~1.4 | A + UAT | ☐ |

---

## Epic 레벨 DoD

- [ ] **`E2EDataSimulator`** — 1주 분량 수주 자동 생성
- [ ] **Cascade 시나리오**: 수주 → VC → EX 자동 chain
- [ ] **모든 납기 D-Day 충족** 검증
- [ ] **베타 사용자 4명 설정** (Keycloak + 권한)
- [ ] **NS-01 사전 설문** (현 만족도 baseline)
- [ ] **1주 병행 운영 가이드** 문서
- [ ] Phase 3 진입 가능 상태

---

## References

- **WBS**: §8 EP-E2E
- **EXP**: EXP-1·2·5
- **TestPlan**: E2E 시나리오
- **선행**: 모든 Sprint 1~5 Epic
- **후행**: Phase 3

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §8 EP-E2E |
