# Epic Overview — [EP-13] (v1.4 재정의) 당일 락 강제 ⭐

**Sprint**: S4 | **Priority**: Must ⭐⭐ (Sprint 4 핵심) | **SP 합계**: 8 | **PD 추정**: ~5.6 PD

---

## Epic 목적

> WBS §7 EP-13 인용: "v1.4 재정의 — 일중 셋팅 교체 차단 + 일말 경계 + 사용자 override"
> SRS REQ-FUNC-VC-012·013·014 / BR-V07 / ADR-016 / PDD-02 v1.1: "당일(영업일) 내에서 가류기 셋팅 변경은 금지되며, 영업일 경계(일말)에서만 교체 가능. override 요청 시 사유 강제 입력 + audit."

본 Epic은 **Sprint 4 가장 중요한 v1.4 신규 규칙**. R-V07 (일중 교체 손실) 직접 차단 — 셋팅 교체 1회 평균 45분 손실. ADR-016에 따라 4-layer 강제:
1. **DB UNIQUE 제약** (당일 락 가드레일) — INSERT 시점 차단
2. **RuleEngine `intra_day_lock_ok`** — Allocator 후보 생성 시 차단
3. **일말 경계 + DO-04** 영업일 경계 키 출력
4. **사용자 override 모달 + 사유 강제**

**Why Sprint 4 핵심**:
- **R-V07 hard** — 일중 교체 0건 1주 회귀 100%
- **BR-V07·VC-012·013·014** — v1.4 4개 SRS REQ-FUNC 통합 완성
- **EP-10·EP-11 거버넌스 활용** — 확정 게이트 + audit 결합

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-13-1](ST-13-1/_Story_Overview.md) | DB UNIQUE 제약 (당일 락 가드레일) | 3 | ~2.1 | T-I + A | ☐ |
| [ST-13-2](ST-13-2/_Story_Overview.md) | RuleEngine 일중 교체 차단 | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-13-3](ST-13-3/_Story_Overview.md) | 일말 교체 경계 + DO-04 영업일 키 출력 | 1 | ~0.7 | T-U + T-I | ☐ |
| [ST-13-4](ST-13-4/_Story_Overview.md) | 사용자 override 모달 + 사유 강제 | 1 | ~0.7 | T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **DB UNIQUE 제약** `(machine_id, slot_position, production_date, hose_id) DEFERRABLE INITIALLY DEFERRED`
- [ ] **RuleEngine `IntraDayLockRule.evaluate()`** — 후보 생성 시 차단
- [ ] **DO-04 출력**: 영업일 경계 키 형식 (`YYYY-MM-DD_END`)
- [ ] **Override 모달 UI** — 사유 텍스트 강제 (REQ-FUNC-CO-010 정합)
- [ ] **1주 호라이즌 회귀**: 일중 교체 0건
- [ ] **마이그레이션 사전 점검 SQL** — 기존 데이터 위반 row 검출
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-13
- **PDD-02 v1.1**: BR-V07 (당일 락)
- **SRS REQ-FUNC**: VC-012 (UNIQUE), VC-013 (일말 경계), VC-014 (override + 사유)
- **SAD**: ADR-016 (4-layer 강제)
- **TestPlan**: TC-VC-012·013·014
- **선행**: EP-05 (VcSchedule), EP-10 (확정 게이트), EP-11 (audit)
- **후행**: Sprint 5 UI 통합

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-13 v1.4 재정의 + 4 SRS REQ + ADR-016 |
